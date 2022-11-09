package sweet.delights.anonymization

case class PII[H <: Hash](hash: H) extends scala.annotation.StaticAnnotation
