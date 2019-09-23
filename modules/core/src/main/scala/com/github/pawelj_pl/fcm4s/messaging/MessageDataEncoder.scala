package com.github.pawelj_pl.fcm4s.messaging

trait MessageDataEncoder[A] {
  def encode(data: A): MessageData
}

object MessageDataEncoder {
  def apply[A](implicit ev: MessageDataEncoder[A]): MessageDataEncoder[A] = ev

  implicit val stringMapMessageDataEncoder: MessageDataEncoder[Map[String, String]] = new MessageDataEncoder[Map[String, String]] {
    override def encode(data: Map[String, String]): MessageData = data
  }

  def deriveFrom[A](fn: A => MessageData): MessageDataEncoder[A] = (data: A) => fn(data)
}
