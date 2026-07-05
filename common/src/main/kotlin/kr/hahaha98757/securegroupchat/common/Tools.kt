package kr.hahaha98757.securegroupchat.common

import kotlin.system.exitProcess

/** 서버 포트. */
const val PORT = 51923

/** [throwable]의 스택 트레이스를 출력하고, [text]를 표준 오류 스트림에 출력합니다. */
fun printErr(text: String, throwable: Throwable) {
    throwable.printStackTrace()
    System.err.println(text)
}

/** [text]를 디버그로 표준 출력 스트림에 출력합니다. */
fun printDebug(text: String) = println("[DEBUG] $text")

/**
 * 프로그램을 종료합니다. 종료 전 1초 또는 5초의 대기 시간을 갖습니다.
 *
 * @param code 종료 코드. 0이면 정상 종료, 0이 아니면 비정상 종료입니다.
 */
fun exit(code: Int): Nothing = if (code == 0) {
    println("1초 뒤, 프로그램이 종료됩니다.")
    Thread.sleep(1000)
    exitProcess(0)
} else {
    println("5초 뒤, 프로그램이 종료됩니다. (종료 코드: $code)")
    Thread.sleep(5000)
    exitProcess(code)
}

/** [text]를 명령어 사용법으로 출력합니다. */
fun printCommandUsage(text: String) {
    println("사용법: $text")
}

/** 명령어 도움말을 출력합니다. */
fun commandHelp() {
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

/**
 * 문자열이 유효하지 않은지 확인합니다.
 *
 * 유효하지 않은 문자열은 비어있거나, 알파벳 대소문자, 숫자, 언더바(_) 외의 문자를 포함하는 문자열입니다.
 *
 * @return 유효하지 않으면 true, 유효하면 false.
 */
fun String.isNotValid() = this.isEmpty() || this.replace(Regex("[A-Za-z0-9_]"), "").isNotEmpty()