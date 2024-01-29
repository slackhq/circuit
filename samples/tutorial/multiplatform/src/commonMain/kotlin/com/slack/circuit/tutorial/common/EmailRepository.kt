package com.slack.circuit.tutorial.common

class EmailRepository {
  companion object {
    val DEMO = Email(
      id = "1",
      subject = "Meeting re-sched!",
      body = "Hey, I'm going to be out of the office tomorrow. Can we reschedule our meeting for Thursday or next week?",
      sender = "Ali Connors",
      timestamp = "3:00 PM",
      recipients = listOf("all@example.com")
    )
  }

  private val emails = listOf(DEMO).associateBy { it.id }

  fun getEmails(): List<Email> {
    return emails.values.toList()
  }

  fun getEmail(id: String): Email {
    return emails.getValue(id)
  }
}