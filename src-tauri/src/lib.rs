mod installed_apps;
mod permissions;
mod storage;

use installed_apps::list_installed_apps;
use permissions::{get_service_status, open_service_settings};
use storage::{AppState, CreateAppInput, FrictionApp, UpdateAppInput};
use tauri::{Manager, State};

#[tauri::command]
fn list_apps(state: State<'_, AppState>) -> Result<Vec<FrictionApp>, String> {
    storage::list_apps(state.db_path()).map_err(|err| err.to_string())
}

#[tauri::command]
fn get_app(state: State<'_, AppState>, id: String) -> Result<Option<FrictionApp>, String> {
    storage::get_app(state.db_path(), &id).map_err(|err| err.to_string())
}

#[tauri::command]
fn create_app(state: State<'_, AppState>, input: CreateAppInput) -> Result<FrictionApp, String> {
    storage::create_app(state.db_path(), input).map_err(|err| err.to_string())
}

#[tauri::command]
fn update_app(
    state: State<'_, AppState>,
    id: String,
    patch: UpdateAppInput,
) -> Result<FrictionApp, String> {
    storage::update_app(state.db_path(), &id, patch).map_err(|err| err.to_string())
}

#[tauri::command]
fn delete_app(state: State<'_, AppState>, id: String) -> Result<(), String> {
    storage::delete_app(state.db_path(), &id).map_err(|err| err.to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            let db_path = app.path().app_data_dir()?.join("friction-timer.db");
            storage::initialize_database(&db_path)
                .map_err(|err| -> Box<dyn std::error::Error> { Box::new(err) })?;
            app.manage(AppState::new(db_path));
            Ok(())
        })
        .plugin(installed_apps::init())
        .plugin(permissions::init())
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![
            list_apps,
            get_app,
            create_app,
            update_app,
            delete_app,
            list_installed_apps,
            get_service_status,
            open_service_settings
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
