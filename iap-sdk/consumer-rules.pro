# Consumer ProGuard/R8 rules applied automatically to apps that depend on this SDK.
# Keep the public, developer-facing API surface stable under obfuscation.
-keep public class com.example.iapsdk.ui.PurchaseDialog { public *; }
-keep public class com.example.iapsdk.model.PurchasePopupData { *; }
-keep public interface com.example.iapsdk.callbacks.PurchasePopupCallback { *; }
