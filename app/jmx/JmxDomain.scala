package jmx

import javax.management.{ObjectInstance, MBeanServerConnection, ObjectName}

/** @author Stephen Samuel */
class JmxDomain(val name: String, conn: MBeanServerConnection) {

  def objectNames: Seq[ObjectName] = conn.queryNames(new ObjectName(name + ":*"), null).asScala.toSeq

  def objectInstances: Seq[ObjectInstance] = conn.queryMBeans(new ObjectName(name), null).asScala.toSeq

  /**
   * Returns all MBeans for this JmxDomain at the time of invocation.
   *
   * Each returned MBean is immutable with respect to the list of attributes.
   * If the list of published MBeans on the domain
   * changes then this method will need to be re-invoked.
   *
   * @return all MBeans for this domain
   */
  def mbeans: Seq[MBean] = _mbeans(name + ":*").sortBy(_.objectName)

  /**
   * Returns all MBeans that match the given key and value.
   *
   * @param key
   * @param value
   * @return
   */
  def mbeans(key: String, value: String): Seq[MBean] = _mbeans(s"$name:$key=$value,*")

  /**
   * Returns the first MBean that matches the given key and value
   *
   * @param key
   * @param value
   * @return
   */
  def mbean(key: String, value: String): Option[MBean] = mbeans(key, value).headOption

  def mbeanNames: Seq[String] = conn.queryMBeans(new ObjectName(name), null).asScala.map(_.getObjectName.toString).toSeq

  def _mbeans(name: String): Seq[MBean] = conn.queryMBeans(new ObjectName(name), null).asScala.map(new MBean(_, conn)).toSeq

  override def toString = "[JmxDomain name=" + name + "]"
}
