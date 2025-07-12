package top.maary.emojiface.domain.usecase

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.datastore.PreferenceRepository
import javax.inject.Inject

class ManageAppIconVisibilityUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferenceRepository: PreferenceRepository
) {

    /**
     * 设置应用启动器图标（通过别名）的可见性。
     *
     * @param hide True 表示隐藏图标，False 表示显示图标。
     * @return Result<Unit> 指示操作是否成功。
     */
    suspend operator fun invoke(hide: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val packageManager = context.packageManager
            // 确保 AndroidManifest.xml 中定义了 <activity-alias> 并且其 name 与这里匹配
            val componentName = ComponentName(context, "${context.packageName}.MainActivityAlias")

            val newState = if (hide) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }

            // 尝试更改组件状态
            packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP // 不杀死应用进程
            )

            // 只有在系统设置成功后才更新偏好设置
            preferenceRepository.updateIconState(hide)

            // 不需要显式返回值，runCatching 会包装 Unit
        }.onFailure {
            // 可以在这里记录更详细的日志
            android.util.Log.e("IconVisibilityUseCase", "Failed to set component state", it)
        }
    }
}
