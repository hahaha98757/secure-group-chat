package kr.hahaha98757.securegroupchat.common

import kotlin.system.exitProcess

const val PORT = 51923

fun printErr(text: String, e: Throwable) {
    e.printStackTrace()
    System.err.println(text)
}

fun printDebug(text: String) {
    println("[DEBUG] $text")
}

fun exit(code: Int): Nothing = if (code == 0) {
    println("1초 뒤, 프로그램이 종료됩니다.")
    Thread.sleep(1000)
    exitProcess(0)
} else {
    println("5초 뒤, 프로그램이 종료됩니다. (종료 코드: $code)")
    Thread.sleep(5000)
    exitProcess(code)
}

fun printUsage(text: String) {
    println("잘못된 구문: $text")
}

fun help() {
    println("""
        rename <name> - 이름을 변경합니다.
        list </s | /u> - 세션 또는 유저 목록을 확인합니다. (서버 사용 가능)
        create <name> [password] - 세션을 생성합니다. (비밀번호는 선택 사항)
        join <name> [password] - 세션에 참가합니다. (비밀번호는 선택 사항)
        request [/a | /r] [name] - 세션 참가 요청을 확인하거나 수락/거절합니다. (세션 주인 전용)
        leave - 세션을 떠납니다.
        kick <name> - 세션에서 유저를 추방합니다. (세션 주인 전용)
        exit - 프로그램을 종료합니다. (서버 사용 가능)
        send - 모든 클라이언트에게 메시지를 전송합니다. (서버 전용)
        help - 도움말을 표시합니다. (서버 사용 가능)
    """.trimIndent())
}

fun String.isNotValid() = this.isEmpty() || this.replace(Regex("[A-Za-z0-9_]"), "").isNotEmpty()