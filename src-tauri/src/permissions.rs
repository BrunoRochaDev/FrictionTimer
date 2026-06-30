use serde::{Deserialize, Serialize};
use tauri::{
    plugin::{Builder, TauriPlugin},
    AppHandle, Manager, Runtime,
};

#[cfg(target_os = "android")]
use tauri::plugin::PluginHandle;

#[cfg(target_os = "android")]
const PLUGIN_IDENTIFIER: &str = "com.brunorochamoura.frictiontimer.permissions";

#[derive(Clone, Copy, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum ServicePermissionKind {
    Overlay,
    Accessibility,
}

#[derive(Clone, Copy, Serialize, Deserialize)]
pub struct ServiceStatus {
    pub overlay: bool,
    pub accessibility: bool,
}

pub struct PermissionsBridge<R: Runtime> {
    #[cfg(not(target_os = "android"))]
    _marker: std::marker::PhantomData<fn() -> R>,
    #[cfg(target_os = "android")]
    mobile_plugin_handle: PluginHandle<R>,
}

impl<R: Runtime> PermissionsBridge<R> {
    fn get_status(&self) -> Result<ServiceStatus, String> {
        #[cfg(target_os = "android")]
        {
            self.mobile_plugin_handle
                .run_mobile_plugin("getStatus", ())
                .map_err(|err| err.to_string())
        }

        #[cfg(not(target_os = "android"))]
        {
            Ok(ServiceStatus {
                overlay: true,
                accessibility: true,
            })
        }
    }

    fn open_settings(&self, kind: ServicePermissionKind) -> Result<(), String> {
        #[cfg(target_os = "android")]
        {
            #[derive(Serialize)]
            struct OpenSettingsRequest {
                kind: ServicePermissionKind,
            }

            self.mobile_plugin_handle
                .run_mobile_plugin::<()>("openSettings", OpenSettingsRequest { kind })
                .map_err(|err| err.to_string())
        }

        #[cfg(not(target_os = "android"))]
        {
            let _ = kind;
            Ok(())
        }
    }
}

pub fn init<R: Runtime>() -> TauriPlugin<R> {
    Builder::new("permissions")
        .setup(|app, _api| {
            #[cfg(target_os = "android")]
            let handle = _api.register_android_plugin(PLUGIN_IDENTIFIER, "PermissionsPlugin")?;

            app.manage(PermissionsBridge::<R> {
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
pub fn get_service_status<R: Runtime>(app: AppHandle<R>) -> Result<ServiceStatus, String> {
    app.state::<PermissionsBridge<R>>().get_status()
}

#[tauri::command]
pub fn open_service_settings<R: Runtime>(
    app: AppHandle<R>,
    kind: ServicePermissionKind,
) -> Result<(), String> {
    app.state::<PermissionsBridge<R>>().open_settings(kind)
}
