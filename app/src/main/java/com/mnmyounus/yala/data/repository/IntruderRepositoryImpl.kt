package com.mnmyounus.yala.data.repository

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.mnmyounus.yala.data.local.IntruderDao
import com.mnmyounus.yala.data.local.IntruderEntity
import com.mnmyounus.yala.domain.model.CaptureOutcome
import com.mnmyounus.yala.domain.model.IntruderShot
import com.mnmyounus.yala.domain.repository.IntruderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Silent front-camera capture. No preview surface is attached, so nothing is
 * shown on screen. Images are written to app-private storage only — they never
 * leave the device and never appear in the system gallery.
 */
@Singleton
class IntruderRepositoryImpl @Inject constructor(
    private val context: Context,
    private val dao: IntruderDao
) : IntruderRepository {

    /** Set by LockScreenActivity so CameraX has a lifecycle to bind to. */
    @Volatile var lifecycleOwner: LifecycleOwner? = null

    override fun shots(outcome: CaptureOutcome): Flow<List<IntruderShot>> =
        dao.observeByOutcome(outcome.name).map { list -> list.map { it.toDomain() } }

    override suspend fun capture(packageName: String, outcome: CaptureOutcome) {
        val owner = lifecycleOwner ?: return
        val dir = File(context.filesDir, "intruders/${outcome.name.lowercase()}").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val file = File(dir, "$stamp.jpg")

        val ok = takeSilentPhoto(owner, file)
        if (!ok) return

        dao.insert(
            IntruderEntity(
                filePath = file.absolutePath,
                packageName = packageName,
                timestampMillis = System.currentTimeMillis(),
                outcome = outcome.name
            )
        )
    }

    private suspend fun takeSilentPhoto(owner: LifecycleOwner, file: File): Boolean =
        suspendCancellableCoroutine { cont ->
            runCatching {
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    runCatching {
                        val provider = future.get()
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        provider.unbindAll()
                        provider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, capture)

                        val output = ImageCapture.OutputFileOptions.Builder(file).build()
                        capture.takePicture(
                            output,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                                    provider.unbindAll()
                                    if (cont.isActive) cont.resume(true)
                                }

                                override fun onError(e: ImageCaptureException) {
                                    provider.unbindAll()
                                    if (cont.isActive) cont.resume(false)
                                }
                            }
                        )
                    }.onFailure { if (cont.isActive) cont.resume(false) }
                }, ContextCompat.getMainExecutor(context))
            }.onFailure { if (cont.isActive) cont.resume(false) }
        }

    override suspend fun delete(id: Long) {
        dao.byId(id)?.let { File(it.filePath).delete() }
        dao.delete(id)
    }

    override suspend fun clear(outcome: CaptureOutcome) {
        dao.allByOutcome(outcome.name).forEach { File(it.filePath).delete() }
        dao.deleteByOutcome(outcome.name)
    }

    private fun IntruderEntity.toDomain() = IntruderShot(
        id = id,
        filePath = filePath,
        packageName = packageName,
        timestampMillis = timestampMillis,
        outcome = CaptureOutcome.valueOf(outcome)
    )
}
