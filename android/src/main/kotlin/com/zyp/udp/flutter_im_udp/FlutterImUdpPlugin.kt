package com.zyp.udp.flutter_im_udp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import net.x52im.mobileimsdk.android.ClientCoreSDK
import net.x52im.mobileimsdk.android.conf.ConfigEntity
import net.x52im.mobileimsdk.android.core.AutoReLoginDaemon
import net.x52im.mobileimsdk.android.core.KeepAliveDaemon
import net.x52im.mobileimsdk.android.core.LocalDataSender
import net.x52im.mobileimsdk.android.core.LocalDataSender.SendCommonDataAsync
import net.x52im.mobileimsdk.android.core.LocalDataSender.SendLoginDataAsync
import net.x52im.mobileimsdk.android.core.QoS4ReciveDaemon
import net.x52im.mobileimsdk.android.core.QoS4SendDaemon
import net.x52im.mobileimsdk.android.utils.MBAsyncTask
import net.x52im.mobileimsdk.server.protocal.c.PLoginInfo
import java.lang.ref.WeakReference
import java.util.Observable
import java.util.Observer

/** FlutterImUdpPlugin */
class FlutterImUdpPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private var channel: MethodChannel? = null

  /**
   * MobileIMSDK是否已被初始化. true表示已初化完成，否则未初始化.
   */
  private var init = false

  private var context: Context? = null

  companion object {
    val TAG: String = "IMSDKPlugin"
  }

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_im_udp")
    channel!!.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  private fun getLoginInfo(): HashMap<*, *> {
    val dic = HashMap<String, Any>()
    dic["currentLoginUserId"] = ClientCoreSDK.getInstance().currentLoginInfo.loginUserId
    dic["currentLoginToken"] = ClientCoreSDK.getInstance().currentLoginInfo.loginToken
    dic["currentLoginExtra"] = ClientCoreSDK.getInstance().currentLoginInfo.extra
    dic["firstLoginTime"] = ClientCoreSDK.getInstance().currentLoginInfo.firstLoginTime
    return dic
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "getPlatformVersion" -> {
        result.success("Android ${android.os.Build.VERSION.RELEASE}")
      }
      "initMobileIMSDK" -> {
        initMobileIMSDK(call, result)
      }

      "login" -> {
        login(call, result)
      }

      "sendMessage" -> {
        sendMessage(call, result)
      }

      "logout" -> {
        logout(call, result)
      }

      "getConnectedStatus" -> {
        getConnectedStatus(call, result)
      }

      "getCurrentLoginInfo" -> {
        getCurrentLoginInfo(call, result)
      }

      "isAutoReLoginRunning" -> {
        isAutoReLoginRunning(call, result)
      }

      "isKeepAliveRunning" -> {
        isKeepAliveRunning(call, result)
      }

      "isQoS4SendDaemonRunning" -> {
        isQoS4SendDaemonRunning(call, result)
      }

      "isQoS4ReciveDaemonRunning" -> {
        isQoS4ReciveDaemonRunning(call, result)
      }

      else -> result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
    channel!!.setMethodCallHandler(null)
    // 释放IM占用资源
    ClientCoreSDK.getInstance().release()
  }

  // 确保MobileIMSDK被初始化哦（整个APP生生命周期中只需调用一次哦）
  // 提示：在不退出APP的情况下退出登陆后再重新登陆时，请确保调用本方法一次，不然会报code=203错误哦！
  private fun initMobileIMSDK(call: MethodCall, result: Result) {
    if (!init) {
      if (call.arguments is Map<*, *>) {
       val dic: Map<Any, Any> = call.arguments as Map<Any, Any>
        val serverIP = dic["serverIP"] as String?
        val serverPort = dic["serverPort"] as Int?
        val senseMode = dic["senseMode"] as Int?
        val debug = dic["debug"] as Boolean?
        if (serverIP != null && serverPort != null) {
          init = true
          ConfigEntity.serverIP = serverIP
          ConfigEntity.serverPort = serverPort

          ClientCoreSDK.DEBUG = debug === java.lang.Boolean.TRUE
          if (ClientCoreSDK.DEBUG) {
            AutoReLoginDaemon.getInstance().debugObserver =
              createObserverCompletionForDEBUG("AutoReLoginDaemonObserver")
            KeepAliveDaemon.getInstance().debugObserver =
              createObserverCompletionForDEBUG("KeepAliveDaemonObserver")
            QoS4SendDaemon.getInstance().debugObserver =
              createObserverCompletionForDEBUG("QoS4SendDaemonObserver")
            QoS4ReciveDaemon.getInstance().debugObserver =
              createObserverCompletionForDEBUG("QoS4ReciveDaemonObserver")
          }
          if (senseMode != null && senseMode < ConfigEntity.SenseMode.values().size) {
            ConfigEntity.setSenseMode(ConfigEntity.SenseMode.values()[senseMode])
          }
          // 【特别注意】请确保首先进行核心库的初始化（这是不同于iOS和Java端的地方)
          ClientCoreSDK.getInstance().init(this.context)

          ClientCoreSDK.getInstance().chatBaseEvent = ChatBaseEventImpl(channel)
          ClientCoreSDK.getInstance().chatMessageEvent = ChatMessageEventImpl(channel)
          ClientCoreSDK.getInstance().messageQoSEvent = MessageQoSEventImpl(channel)

          val resultDic = HashMap<String, Any>()
          resultDic["result"] = java.lang.Boolean.TRUE
          result.success(resultDic)
          return
        }
      }
      val resultDic = HashMap<String, Any>()
      resultDic["result"] = java.lang.Boolean.FALSE
      result.success(resultDic)
    }
  }

  private fun createObserverCompletionForDEBUG(methodName: String): Observer {
    val weakSelf: WeakReference<FlutterImUdpPlugin?> = WeakReference(
      this
    )
    return Observer { o: Observable?, arg: Any? ->
      if (arg != null) {
        val status = arg as Int
        if (weakSelf.get() != null) {
          val mainHandler = Handler(Looper.getMainLooper())
          mainHandler.post { //已在主线程中，可以更新UI
            weakSelf.get()?.channel?.invokeMethod(methodName, status)
          }
        }
      }
    }
  }

  @SuppressLint("StaticFieldLeak")
  private fun login(call: MethodCall, result: Result) {
    if (call.arguments is Map<*, *>) {
      val dic = call.arguments as Map<*, *>
      val loginUserId = dic["loginUserId"] as String?
      val loginToken = dic["loginToken"] as String?

      if (loginUserId != null && loginToken != null) {
        // 异步提交登陆id和token
        object : SendLoginDataAsync(PLoginInfo(loginUserId, loginToken)) {
          /**
           * 登陆信息发送完成后将调用本方法（注意：此处仅是登陆信息发送完成
           * ，真正的登陆结果要在异步回调中处理哦）。
           *
           * @param code 数据发送返回码，0 表示数据成功发出，否则是错误码
           */
          override fun fireAfterSendLogin(code: Int) {
            val resultDic = HashMap<String, Any>()
            if (code == 0) {
              Log.d(TAG, "登陆/连接信息已成功发出！")
              resultDic["result"] = java.lang.Boolean.TRUE
            } else {
              Log.d(TAG, "登陆/连接信息发送失败！")
              resultDic["result"] = java.lang.Boolean.FALSE
            }
            result.success(resultDic)
          }
        }.execute()
        return
      }
    }
    val resultDic = HashMap<String, Any>()
    resultDic["result"] = java.lang.Boolean.FALSE
    result.success(resultDic)
  }

  @SuppressLint("StaticFieldLeak")
  private fun logout(call: MethodCall, result: Result) {
    val weakSelf: WeakReference<FlutterImUdpPlugin?> = WeakReference(
      this
    )
    // 发出退出登陆请求包（Android系统要求必须要在独立的线程中发送哦）
    object : MBAsyncTask() {
      override fun doInBackground(vararg params: Any): Int {
        var code = -1
        try {
          code = LocalDataSender.getInstance().sendLoginout()
        } catch (e: Exception) {
          Log.w(TAG, e)
        }

        //## BUG FIX: 20170713 START by JackJiang
        // 退出登陆时记得一定要调用此行，不然不退出APP的情况下再登陆时会报 code=203错误哦！
        if (weakSelf.get() != null) {
          weakSelf.get()?.init = false
        }

        //## BUG FIX: 20170713 END by JackJiang
        return code
      }

      override fun onPostExecute(code: Int) {
        val resultDic = HashMap<String, Any>()
        if (code == 0) resultDic["result"] = java.lang.Boolean.TRUE
        else {
          resultDic["result"] = java.lang.Boolean.FALSE
        }
        resultDic["value"] = code
        result.success(resultDic)
      }
    }.execute()
  }

  @SuppressLint("StaticFieldLeak")
  private fun sendMessage(call: MethodCall, result: Result) {
    if (call.arguments is Map<*, *>) {
      val dic = call.arguments as Map<*, *>
      val dataContent = dic["dataContent"] as String?
      val toUserId = dic["toUserId"] as String?
      val fingerPrint = dic["fingerPrint"] as String?
      val typeu = dic["typeu"] as Int?
      if (dataContent != null && toUserId != null) {
        // 发送消息（Android系统要求必须要在独立的线程中发送哦）
        object : SendCommonDataAsync(dataContent, toUserId, fingerPrint, typeu ?: -1) {
          override fun onPostExecute(code: Int) {
            val resultDic = HashMap<String, Any>()
            if (code == 0) {
              resultDic["result"] = java.lang.Boolean.TRUE
            } else {
              resultDic["result"] = java.lang.Boolean.FALSE
            }
            result.success(resultDic)
          }
        }.execute()
        return
      }
    }

    val resultDic = HashMap<String, Any>()
    resultDic["result"] = java.lang.Boolean.FALSE
    result.success(resultDic)
  }

  private fun getConnectedStatus(call: MethodCall, result: Result) {
    // 获取与服务器连接状态
    val dic = HashMap<String, Any>()
    dic["result"] = java.lang.Boolean.TRUE
    dic["value"] = ClientCoreSDK.getInstance().isConnectedToServer
    result.success(dic)
  }

  private fun getCurrentLoginInfo(call: MethodCall, result: Result) {
    // 获取当前登录信息
    val dic = HashMap<String, Any>()
    dic["result"] = ClientCoreSDK.getInstance().currentLoginInfo != null
    dic["value"] = getLoginInfo()
    result.success(dic)
  }

  private fun isAutoReLoginRunning(call: MethodCall, result: Result) {
    // 自动登录重连是否正在运行
    val dic = HashMap<String, Any>()
    dic["result"] = java.lang.Boolean.TRUE
    dic["value"] = AutoReLoginDaemon.getInstance().isAutoReLoginRunning
    result.success(dic)
  }

  private fun isKeepAliveRunning(call: MethodCall, result: Result) {
    // keepAlive是否正在运行
    val dic = HashMap<String, Any>()
    dic["result"] = java.lang.Boolean.TRUE
    dic["value"] = KeepAliveDaemon.getInstance().isKeepAliveRunning
    result.success(dic)
  }

  private fun isQoS4SendDaemonRunning(call: MethodCall, result: Result) {
    // QoS4SendDaemon是否正在运行
    val dic = HashMap<String, Any>()
    dic["result"] = java.lang.Boolean.TRUE
    dic["value"] = QoS4SendDaemon.getInstance().isRunning
    result.success(dic)
  }

  private fun isQoS4ReciveDaemonRunning(call: MethodCall, result: Result) {
    // QoS4ReciveDaemon是否正在运行
    val dic = HashMap<String, Any>()
    dic["result"] = java.lang.Boolean.TRUE
    dic["value"] = QoS4ReciveDaemon.getInstance().isRunning
    result.success(dic)
  }
}
