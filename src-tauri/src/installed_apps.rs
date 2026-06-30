use serde::{Deserialize, Serialize};
use tauri::{
    plugin::{Builder, TauriPlugin},
    AppHandle, Manager, Runtime,
};

#[cfg(target_os = "android")]
use tauri::plugin::PluginHandle;

#[cfg(target_os = "android")]
const PLUGIN_IDENTIFIER: &str = "com.brunorochamoura.friction_timer.installedapps";

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct InstalledApp {
    pub app_id: String,
    pub name: String,
}

pub struct InstalledAppsBridge<R: Runtime> {
    #[cfg(not(target_os = "android"))]
    _marker: std::marker::PhantomData<fn() -> R>,
    #[cfg(target_os = "android")]
    mobile_plugin_handle: PluginHandle<R>,
}

impl<R: Runtime> InstalledAppsBridge<R> {
    fn list_installed_apps(&self, query: Option<String>) -> Result<Vec<InstalledApp>, String> {
        #[cfg(target_os = "android")]
        let apps = self
            .mobile_plugin_handle
            .run_mobile_plugin::<Vec<InstalledApp>>("listInstalledApps", ())
            .map_err(|err| err.to_string())?;

        #[cfg(not(target_os = "android"))]
        let apps = desktop_installed_apps();

        Ok(filter_apps(apps, query.as_deref()))
    }
}

pub fn init<R: Runtime>() -> TauriPlugin<R> {
    Builder::new("installed-apps")
        .setup(|app, _api| {
            #[cfg(target_os = "android")]
            let handle = _api.register_android_plugin(PLUGIN_IDENTIFIER, "InstalledAppsPlugin")?;

            app.manage(InstalledAppsBridge::<R> {
                #[cfg(not(target_os = "android"))]
                _marker: std::marker::PhantomData,
                #[cfg(target_os = "android")]
                mobile_plugin_handle: handle,
            });

            Ok(())
        })
        .build()
}

#[tauri::command]
pub fn list_installed_apps<R: Runtime>(
    app: AppHandle<R>,
    query: Option<String>,
) -> Result<Vec<InstalledApp>, String> {
    app.state::<InstalledAppsBridge<R>>()
        .list_installed_apps(query)
}

#[cfg(not(target_os = "android"))]
fn desktop_installed_apps() -> Vec<InstalledApp> {
    vec![
        InstalledApp {
            app_id: "com.instagram.android".into(),
            name: "Instagram".into(),
        },
        InstalledApp {
            app_id: "com.zhiliaoapp.musically".into(),
            name: "TikTok".into(),
        },
        InstalledApp {
            app_id: "com.twitter.android".into(),
            name: "X".into(),
        },
        InstalledApp {
            app_id: "com.reddit.frontpage".into(),
            name: "Reddit".into(),
        },
        InstalledApp {
            app_id: "com.google.android.youtube".into(),
            name: "YouTube".into(),
        },
        InstalledApp {
            app_id: "com.facebook.katana".into(),
            name: "Facebook".into(),
        },
        InstalledApp {
            app_id: "com.snapchat.android".into(),
            name: "Snapchat".into(),
        },
        InstalledApp {
            app_id: "com.netflix.mediaclient".into(),
            name: "Netflix".into(),
        },
        InstalledApp {
            app_id: "com.discord".into(),
            name: "Discord".into(),
        },
        InstalledApp {
            app_id: "com.whatsapp".into(),
            name: "WhatsApp".into(),
        },
        InstalledApp {
            app_id: "com.spotify.music".into(),
            name: "Spotify".into(),
        },
        InstalledApp {
            app_id: "com.amazon.mShop.android.shopping".into(),
            name: "Amazon".into(),
        },
    ]
}

fn filter_apps(mut apps: Vec<InstalledApp>, query: Option<&str>) -> Vec<InstalledApp> {
    let query = query.unwrap_or_default().trim().to_lowercase();

    if !query.is_empty() {
        apps.retain(|app| {
            app.name.to_lowercase().contains(&query) || app.app_id.to_lowercase().contains(&query)
        });
    }

    apps.sort_by(|left, right| {
        left.name
            .to_lowercase()
            .cmp(&right.name.to_lowercase())
            .then_with(|| left.app_id.cmp(&right.app_id))
    });

    apps
}

#[cfg(test)]
mod tests {
    use super::{filter_apps, InstalledApp};

    fn sample_apps() -> Vec<InstalledApp> {
        vec![
            InstalledApp {
                app_id: "com.spotify.music".into(),
                name: "spotify".into(),
            },
            InstalledApp {
                app_id: "com.instagram.android".into(),
                name: "Instagram".into(),
            },
            InstalledApp {
                app_id: "org.mozilla.firefox".into(),
                name: "Firefox".into(),
            },
            InstalledApp {
                app_id: "com.google.android.youtube".into(),
                name: "YouTube".into(),
            },
        ]
    }

    #[test]
    fn empty_query_returns_all_apps_sorted() {
        let apps = filter_apps(sample_apps(), None);

        assert_eq!(
            apps.into_iter().map(|app| app.name).collect::<Vec<_>>(),
            vec!["Firefox", "Instagram", "spotify", "YouTube"]
        );
    }

    #[test]
    fn search_is_case_insensitive() {
        let apps = filter_apps(sample_apps(), Some("SPOT"));

        assert_eq!(apps.len(), 1);
        assert_eq!(apps[0].app_id, "com.spotify.music");
    }

    #[test]
    fn search_matches_name_and_package_id() {
        let by_name = filter_apps(sample_apps(), Some("fire"));
        let by_id = filter_apps(sample_apps(), Some("google.android"));

        assert_eq!(by_name[0].app_id, "org.mozilla.firefox");
        assert_eq!(by_id[0].app_id, "com.google.android.youtube");
    }

    #[test]
    fn sort_is_deterministic_for_matching_names() {
        let apps = vec![
            InstalledApp {
                app_id: "com.beta".into(),
                name: "Same".into(),
            },
            InstalledApp {
                app_id: "com.alpha".into(),
                name: "same".into(),
            },
        ];

        let apps = filter_apps(apps, None);

        assert_eq!(
            apps.into_iter().map(|app| app.app_id).collect::<Vec<_>>(),
            vec!["com.alpha", "com.beta"]
        );
    }
}
