package com.example.touchmirror

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(24))
            setBackgroundColor(Color.rgb(247, 248, 250))
        }
        root.addView(TextView(this).apply {
            text = "Touch Mirror"
            textSize = 28f
            setTextColor(Color.rgb(28, 38, 52))
        }, matchWrap())
        root.addView(TextView(this).apply {
            text = "Dùng 1/3 màn hình phía dưới làm vùng điều khiển thay thế cho 1/3 phía trên. Bản thử nghiệm này mô phỏng cử chỉ sau khi bạn nhấc ngón tay."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(12), 0, dp(18))
        }, matchWrap())

        root.addView(actionButton("1. Mở cài đặt Trợ năng") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(actionButton("2. Bật Touch Mirror trong danh sách dịch vụ") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(actionButton("3. Bắt đầu vùng điều khiển") {
            if (TouchMirrorService.instance != null) {
                TouchMirrorService.instance?.showOverlay()
                Toast.makeText(this, "Đã yêu cầu hiển thị vùng điều khiển", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Hãy bật dịch vụ Touch Mirror trong Cài đặt Trợ năng trước.", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })
        root.addView(actionButton("Dừng vùng điều khiển") {
            sendBroadcast(Intent(StopReceiver.ACTION_STOP).setPackage(packageName))
            Toast.makeText(this, "Đã yêu cầu dừng vùng điều khiển", Toast.LENGTH_SHORT).show()
        })
        root.addView(TextView(this).apply {
            text = "Giới hạn: Android không cung cấp chuyển tiếp cảm ứng thô toàn hệ thống cho ứng dụng thông thường. Bản thử nghiệm ghi lại một cử chỉ trong vùng dưới rồi gửi cử chỉ mô phỏng lên vùng trên khi nhấc ngón tay. Kéo/vuốt sẽ không phản hồi trực tiếp theo thời gian thực."
            textSize = 13f
            setTextColor(Color.rgb(95, 100, 110))
            setPadding(0, dp(20), 0, 0)
        }, matchWrap())
        setContentView(root)
    }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
