package jmx

import javax.management._

/**
 * An immutable instance of a managed bean. The attribute and operation lists are fetched on creation.
 * If the mbean changes its published attributes and operations then a new instance will need to be
 * requested from the JmxDomain.
 *
 * @author Stephen Samuel
 *
 **/
class MBean(instance: ObjectInstance, conn: MBeanServerConnection) {

  val objectName = instance.getObjectName
  lazy val canonicalName = objectName.getCanonicalName
  lazy val info = conn.getMBeanInfo(objectName)
  val attributes: Seq[MBeanAttribute] = info.getAttributes.map(arg => {
    new MBeanAttribute(conn, this, arg.getName, arg.getType, arg.getDescription, arg.isReadable, arg.isWritable)
  })

  /**
   * Returns the attribute with the given name if it exists.
   *
   * @param name the name of the attribute to fetch
   * @return An option containing the MBeanAttribute for the given name.
   */
  def attribute(name: String): Option[MBeanAttribute] = attributes.find(_.name == name)

  def attributes(names: Array[String]): Seq[MBeanAttribute] = attributes.filter(attr => names.contains(attr.name))

  val operations: Seq[MBeanOperation] = info.getOperations.map(arg => {
    new MBeanOperation(conn, this, arg.getName, arg.getReturnType, arg.getSignature, arg.getDescription, _impact(arg.getImpact))
  })

  def operation(name: String): Option[MBeanOperation] = operations.find(_.name == name)

  def _impact(i: Int): Impact = i match {
    case MBeanOperationInfo.ACTION => Impact.Action
    case MBeanOperationInfo.INFO => Impact.Info
    case MBeanOperationInfo.ACTION_INFO => Impact.ActionInfo
    case _ => Impact.Uknown
  }

  /**
   * Returns all the attribute names as a sequence of Strings.
   * @return
   */
  def attributeNames: Seq[String] = attributes.map(_.name)

  def attributeValues: Seq[AnyRef] = attributeValues(attributeNames.toArray)

  def attributeValues(names: Array[String]): Seq[AnyRef] = attributes(names).map(_.value)

  def attributeValues(attrs: Seq[MBeanAttribute]): Seq[AnyRef] = attributeValues(attrs.map(_.name).toArray)

  /**
   * Adds a notification listener to receive events originating from this bean
   *
   * @param listener the listener to register
   */
  def addNotificationListener(listener: NotificationListener) {
    conn.addNotificationListener(objectName, listener, null, null)
  }

  /**

   * Removes the given notification listener from listening to events from this MBean
   *
   * @param listener the listener to remove
   */
  def removeNotificationListener(listener: NotificationListener) {
    conn.removeNotificationListener(objectName, listener)
  }

  override def toString = s"[MBean name=$objectName]"
}

class MBeanAttribute(conn: MBeanServerConnection,
                     mbean: MBean,
                     val name: String,
                     val className: String,
                     val description: String,
                     val readable: Boolean,
                     val writable: Boolean) {
  def value: AnyRef = conn.getAttribute(mbean.objectName, name)

  def value_$eq(value: AnyRef) {
    conn.setAttribute(mbean.objectName, new Attribute(name, value))
  }

  override def toString = s"[MBeanAttribute name=$name]"
}

class MBeanOperation(conn: MBeanServerConnection,
                     mbean: MBean,
                     val name: String,
                     val returnType: String,
                     val signature: Seq[MBeanParameterInfo],
                     val description: String,
                     val impact: Impact) {
  def invoke[T](params: AnyRef*): T = conn
    .invoke(mbean.objectName, name, params.toArray, signature.map(_.getType).toArray)
    .asInstanceOf[T]

  def invoke[T]: T = invoke()

  override def toString = s"[MBeanOperation name=$name]"
}
