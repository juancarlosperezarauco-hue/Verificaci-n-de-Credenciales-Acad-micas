package bo.edu.uagrm.credential_wallet

import android.app.Activity
import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private var pending: MethodChannel.Result? = null
    private var pendingBytes: ByteArray? = null
    private val saveRequest = 4107

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "bo.edu.uagrm.credentials/documents")
            .setMethodCallHandler { call, result ->
                if (call.method != "savePdf") {
                    result.notImplemented()
                } else if (pending != null) {
                    result.error("BUSY", "Ya hay un guardado pendiente.", null)
                } else {
                    val bytes = call.argument<ByteArray>("bytes")
                    if (bytes == null || bytes.size < 5 || bytes.size > 11 * 1024 * 1024 ||
                        String(bytes.copyOfRange(0, 4), Charsets.US_ASCII) != "%PDF") {
                        result.error("INVALID_PDF", "El archivo no es un PDF válido.", null)
                        return@setMethodCallHandler
                    }
                    val name = (call.argument<String>("name") ?: "credencial.pdf")
                        .replace("/", "_").replace("\\", "_").take(120)
                    pending = result
                    pendingBytes = bytes
                    try {
                        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_TITLE, name)
                        }, saveRequest)
                    } catch (_: Exception) {
                        pending = null
                        pendingBytes = null
                        result.error("SAVE_UNAVAILABLE", "No se pudo abrir el selector de archivos.", null)
                    }
                }
            }
    }

    @Deprecated("Used by the Android document picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != saveRequest) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        val reply = pending
        val bytes = pendingBytes
        pending = null
        pendingBytes = null
        val uri = data?.data
        if (resultCode != Activity.RESULT_OK || uri == null || bytes == null) {
            reply?.success(false)
            return
        }
        try {
            val stream = contentResolver.openOutputStream(uri)
                ?: throw java.io.IOException("No output stream")
            stream.use { it.write(bytes) }
            reply?.success(true)
        } catch (_: Exception) {
            reply?.error("SAVE_FAILED", "No se pudo guardar el PDF.", null)
        }
    }
}
