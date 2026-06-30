use std::fs;
use std::path::{Path, PathBuf};

use rusqlite::{params, Connection, OptionalExtension};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Clone)]
pub struct AppState {
    db_path: PathBuf,
}

impl AppState {
    pub fn new(db_path: PathBuf) -> Self {
        Self { db_path }
    }

    pub fn db_path(&self) -> &Path {
        &self.db_path
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct FrictionApp {
    pub id: String,
    pub app_id: String,
    pub name: String,
    pub wait_seconds: i64,
    pub duration_seconds: i64,
    pub messages: Vec<String>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateAppInput {
    pub app_id: String,
    pub name: String,
    pub wait_seconds: i64,
    pub duration_seconds: i64,
    pub messages: Vec<String>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UpdateAppInput {
    pub app_id: Option<String>,
    pub name: Option<String>,
    pub wait_seconds: Option<i64>,
    pub duration_seconds: Option<i64>,
    pub messages: Option<Vec<String>>,
}

#[derive(Debug)]
pub enum StorageError {
    Io(std::io::Error),
    Sql(rusqlite::Error),
    Json(serde_json::Error),
    Validation(String),
    NotFound(String),
}

impl std::fmt::Display for StorageError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Io(err) => write!(f, "{err}"),
            Self::Sql(err) => write!(f, "{err}"),
            Self::Json(err) => write!(f, "{err}"),
            Self::Validation(message) => write!(f, "{message}"),
            Self::NotFound(message) => write!(f, "{message}"),
        }
    }
}

impl std::error::Error for StorageError {}

impl From<std::io::Error> for StorageError {
    fn from(value: std::io::Error) -> Self {
        Self::Io(value)
    }
}

impl From<rusqlite::Error> for StorageError {
    fn from(value: rusqlite::Error) -> Self {
        Self::Sql(value)
    }
}

impl From<serde_json::Error> for StorageError {
    fn from(value: serde_json::Error) -> Self {
        Self::Json(value)
    }
}

pub fn initialize_database(db_path: &Path) -> Result<(), StorageError> {
    if let Some(parent) = db_path.parent() {
        fs::create_dir_all(parent)?;
    }

    let conn = Connection::open(db_path)?;
    conn.execute_batch(
        "CREATE TABLE IF NOT EXISTS friction_apps (
            id TEXT PRIMARY KEY,
            app_id TEXT NOT NULL,
            name TEXT NOT NULL,
            wait_seconds INTEGER NOT NULL,
            duration_seconds INTEGER NOT NULL,
            messages_json TEXT NOT NULL
        );",
    )?;

    Ok(())
}

pub fn list_apps(db_path: &Path) -> Result<Vec<FrictionApp>, StorageError> {
    let conn = open_connection(db_path)?;
    let mut stmt = conn.prepare(
        "SELECT id, app_id, name, wait_seconds, duration_seconds, messages_json
         FROM friction_apps
         ORDER BY rowid ASC",
    )?;

    let rows = stmt.query_map([], map_row)?;
    let apps = rows.collect::<Result<Vec<_>, _>>()?;
    Ok(apps)
}

pub fn get_app(db_path: &Path, id: &str) -> Result<Option<FrictionApp>, StorageError> {
    let conn = open_connection(db_path)?;
    let app = conn
        .query_row(
            "SELECT id, app_id, name, wait_seconds, duration_seconds, messages_json
             FROM friction_apps
             WHERE id = ?1",
            [id],
            map_row,
        )
        .optional()?;

    Ok(app)
}

pub fn create_app(db_path: &Path, input: CreateAppInput) -> Result<FrictionApp, StorageError> {
    let app = normalize_create_input(input)?;
    let conn = open_connection(db_path)?;

    conn.execute(
        "INSERT INTO friction_apps
            (id, app_id, name, wait_seconds, duration_seconds, messages_json)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
        params![
            app.id,
            app.app_id,
            app.name,
            app.wait_seconds,
            app.duration_seconds,
            serde_json::to_string(&app.messages)?,
        ],
    )?;

    Ok(app)
}

pub fn update_app(
    db_path: &Path,
    id: &str,
    patch: UpdateAppInput,
) -> Result<FrictionApp, StorageError> {
    let current = get_app(db_path, id)?
        .ok_or_else(|| StorageError::NotFound(format!("App not found: {id}")))?;
    let next = apply_patch(current, patch)?;
    let conn = open_connection(db_path)?;

    conn.execute(
        "UPDATE friction_apps
         SET app_id = ?2, name = ?3, wait_seconds = ?4, duration_seconds = ?5, messages_json = ?6
         WHERE id = ?1",
        params![
            next.id,
            next.app_id,
            next.name,
            next.wait_seconds,
            next.duration_seconds,
            serde_json::to_string(&next.messages)?,
        ],
    )?;

    Ok(next)
}

pub fn delete_app(db_path: &Path, id: &str) -> Result<(), StorageError> {
    let conn = open_connection(db_path)?;
    let rows = conn.execute("DELETE FROM friction_apps WHERE id = ?1", [id])?;
    if rows == 0 {
        return Err(StorageError::NotFound(format!("App not found: {id}")));
    }
    Ok(())
}

fn open_connection(db_path: &Path) -> Result<Connection, StorageError> {
    initialize_database(db_path)?;
    Ok(Connection::open(db_path)?)
}

fn map_row(row: &rusqlite::Row<'_>) -> rusqlite::Result<FrictionApp> {
    let messages_json: String = row.get(5)?;
    let messages = serde_json::from_str(&messages_json).map_err(to_sql_error)?;

    Ok(FrictionApp {
        id: row.get(0)?,
        app_id: row.get(1)?,
        name: row.get(2)?,
        wait_seconds: row.get(3)?,
        duration_seconds: row.get(4)?,
        messages,
    })
}

fn normalize_create_input(input: CreateAppInput) -> Result<FrictionApp, StorageError> {
    let id = uuid();
    let app_id = validate_required("appId", input.app_id)?;
    let name = validate_required("name", input.name)?;
    let wait_seconds = validate_seconds("waitSeconds", input.wait_seconds)?;
    let duration_seconds = validate_seconds("durationSeconds", input.duration_seconds)?;
    let messages = normalize_messages(input.messages);

    Ok(FrictionApp {
        id,
        app_id,
        name,
        wait_seconds,
        duration_seconds,
        messages,
    })
}

fn apply_patch(current: FrictionApp, patch: UpdateAppInput) -> Result<FrictionApp, StorageError> {
    let app_id = match patch.app_id {
        Some(value) => validate_required("appId", value)?,
        None => current.app_id,
    };
    let name = match patch.name {
        Some(value) => validate_required("name", value)?,
        None => current.name,
    };
    let wait_seconds = match patch.wait_seconds {
        Some(value) => validate_seconds("waitSeconds", value)?,
        None => current.wait_seconds,
    };
    let duration_seconds = match patch.duration_seconds {
        Some(value) => validate_seconds("durationSeconds", value)?,
        None => current.duration_seconds,
    };
    let messages = match patch.messages {
        Some(values) => normalize_messages(values),
        None => current.messages,
    };

    Ok(FrictionApp {
        id: current.id,
        app_id,
        name,
        wait_seconds,
        duration_seconds,
        messages,
    })
}

fn validate_required(field: &str, value: String) -> Result<String, StorageError> {
    let trimmed = value.trim().to_string();
    if trimmed.is_empty() {
        return Err(StorageError::Validation(format!(
            "{field} must not be empty"
        )));
    }
    Ok(trimmed)
}

fn validate_seconds(field: &str, value: i64) -> Result<i64, StorageError> {
    if value < 1 {
        return Err(StorageError::Validation(format!(
            "{field} must be at least 1"
        )));
    }
    Ok(value)
}

fn normalize_messages(messages: Vec<String>) -> Vec<String> {
    messages
        .into_iter()
        .map(|message| message.trim().to_string())
        .filter(|message| !message.is_empty())
        .collect()
}

fn to_sql_error(err: serde_json::Error) -> rusqlite::Error {
    rusqlite::Error::FromSqlConversionFailure(5, rusqlite::types::Type::Text, Box::new(err))
}

fn uuid() -> String {
    Uuid::new_v4().to_string()
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;
    use std::time::{SystemTime, UNIX_EPOCH};

    fn temp_db_path(name: &str) -> PathBuf {
        let unique = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|duration| duration.as_nanos())
            .unwrap_or_default();
        std::env::temp_dir().join(format!("friction-timer-{name}-{unique}.db"))
    }

    fn sample_input() -> CreateAppInput {
        CreateAppInput {
            app_id: " com.instagram.android ".into(),
            name: " Instagram ".into(),
            wait_seconds: 30,
            duration_seconds: 300,
            messages: vec![
                " Take a breath. ".into(),
                "".into(),
                "Do you need this?".into(),
            ],
        }
    }

    #[test]
    fn initializes_schema() {
        let db_path = temp_db_path("schema");
        initialize_database(&db_path).unwrap();
        let apps = list_apps(&db_path).unwrap();
        assert!(apps.is_empty());
        let _ = fs::remove_file(db_path);
    }

    #[test]
    fn create_list_and_get_round_trip() {
        let db_path = temp_db_path("round-trip");
        let created = create_app(&db_path, sample_input()).unwrap();

        let fetched = get_app(&db_path, &created.id).unwrap().unwrap();
        let listed = list_apps(&db_path).unwrap();

        assert_eq!(created, fetched);
        assert_eq!(listed, vec![created]);
        let _ = fs::remove_file(db_path);
    }

    #[test]
    fn partial_update_changes_only_requested_fields() {
        let db_path = temp_db_path("update");
        let created = create_app(&db_path, sample_input()).unwrap();

        let updated = update_app(
            &db_path,
            &created.id,
            UpdateAppInput {
                app_id: None,
                name: Some(" Reddit ".into()),
                wait_seconds: Some(15),
                duration_seconds: None,
                messages: Some(vec![" Stay focused ".into(), "".into()]),
            },
        )
        .unwrap();

        assert_eq!(updated.id, created.id);
        assert_eq!(updated.app_id, created.app_id);
        assert_eq!(updated.name, "Reddit");
        assert_eq!(updated.wait_seconds, 15);
        assert_eq!(updated.duration_seconds, created.duration_seconds);
        assert_eq!(updated.messages, vec!["Stay focused"]);
        let _ = fs::remove_file(db_path);
    }

    #[test]
    fn delete_removes_row() {
        let db_path = temp_db_path("delete");
        let created = create_app(&db_path, sample_input()).unwrap();

        delete_app(&db_path, &created.id).unwrap();

        assert!(get_app(&db_path, &created.id).unwrap().is_none());
        let _ = fs::remove_file(db_path);
    }

    #[test]
    fn rejects_blank_required_fields() {
        let db_path = temp_db_path("blank-fields");
        let app_id_error = create_app(
            &db_path,
            CreateAppInput {
                app_id: " ".into(),
                ..sample_input()
            },
        )
        .unwrap_err();
        let name_error = create_app(
            &db_path,
            CreateAppInput {
                name: " ".into(),
                ..sample_input()
            },
        )
        .unwrap_err();

        assert!(app_id_error.to_string().contains("appId"));
        assert!(name_error.to_string().contains("name"));
        let _ = fs::remove_file(db_path);
    }

    #[test]
    fn rejects_invalid_durations() {
        let db_path = temp_db_path("durations");
        let wait_error = create_app(
            &db_path,
            CreateAppInput {
                wait_seconds: 0,
                ..sample_input()
            },
        )
        .unwrap_err();
        let duration_error = create_app(
            &db_path,
            CreateAppInput {
                duration_seconds: 0,
                ..sample_input()
            },
        )
        .unwrap_err();

        assert!(wait_error.to_string().contains("waitSeconds"));
        assert!(duration_error.to_string().contains("durationSeconds"));
        let _ = fs::remove_file(db_path);
    }

    #[test]
    fn trims_and_filters_messages() {
        let db_path = temp_db_path("messages");
        let created = create_app(&db_path, sample_input()).unwrap();

        assert_eq!(
            created.messages,
            vec!["Take a breath.", "Do you need this?"]
        );
        let _ = fs::remove_file(db_path);
    }

    #[test]
    fn list_preserves_insertion_order() {
        let db_path = temp_db_path("order");
        let first = create_app(
            &db_path,
            CreateAppInput {
                app_id: "com.first".into(),
                name: "First".into(),
                wait_seconds: 10,
                duration_seconds: 20,
                messages: vec![],
            },
        )
        .unwrap();
        let second = create_app(
            &db_path,
            CreateAppInput {
                app_id: "com.second".into(),
                name: "Second".into(),
                wait_seconds: 30,
                duration_seconds: 40,
                messages: vec![],
            },
        )
        .unwrap();

        let listed = list_apps(&db_path).unwrap();

        assert_eq!(
            listed.into_iter().map(|app| app.id).collect::<Vec<_>>(),
            vec![first.id, second.id]
        );
        let _ = fs::remove_file(db_path);
    }
}
