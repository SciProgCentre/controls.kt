package ru.mipt.npm.controls.xodus

import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.PersistentEntityStore
import jetbrains.exodus.entitystore.PersistentEntityStores
import jetbrains.exodus.entitystore.StoreTransaction
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.mipt.npm.controls.api.DeviceMessage
import ru.mipt.npm.controls.storage.DeviceMessageStorage
import ru.mipt.npm.controls.storage.workDirectory
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Factory
import space.kscience.dataforge.context.fetch
import space.kscience.dataforge.io.IOPlugin
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.matches
import space.kscience.dataforge.names.parseAsName


internal fun StoreTransaction.writeMessage(message: DeviceMessage): Entity {
    val entity: Entity = newEntity(XodusDeviceMessageStorage.DEVICE_MESSAGE_ENTITY_TYPE)
    val json = Json.encodeToJsonElement(DeviceMessage.serializer(), message).jsonObject
    val type = json["type"]?.jsonPrimitive?.content ?: error("Message json representation must have type.")
    entity.setProperty("type", type)

    message.sourceDevice?.let {
        entity.setProperty(DeviceMessage::sourceDevice.name, it.toString())
    }
    message.targetDevice?.let {
        entity.setProperty(DeviceMessage::targetDevice.name, it.toString())
    }
    message.time?.let {
        entity.setProperty(DeviceMessage::targetDevice.name, it.toString())
    }
    entity.setBlobString("json", Json.encodeToString(json))

    return entity
}


@OptIn(DFExperimental::class)
private fun Entity.propertyMatchesName(propertyName: String, pattern: Name? = null) =
    pattern == null || getProperty(propertyName).toString().parseAsName().matches(pattern)

private fun Entity.timeInRange(range: ClosedRange<Instant>?): Boolean {
    if (range == null) return true
    val time: Instant? = getProperty(DeviceMessage::time.name)?.let { entityString ->
        Instant.parse(entityString.toString())
    }
    return time != null && time in range
}

public class XodusDeviceMessageStorage(
    private val entityStore: PersistentEntityStore,
) : DeviceMessageStorage, AutoCloseable {

    override suspend fun write(event: DeviceMessage) {
        //entityStore.encodeToEntity(event, DEVICE_MESSAGE_ENTITY_TYPE, DeviceMessage.serializer())
        entityStore.computeInTransaction { txn ->
            txn.writeMessage(event)
        }
    }

    override suspend fun readAll(): List<DeviceMessage> = entityStore.computeInTransaction { transaction ->
        transaction.getAll(
            DEVICE_MESSAGE_ENTITY_TYPE,
        ).map {
            Json.decodeFromString(
                DeviceMessage.serializer(),
                it.getBlobString("json") ?: error("No json content found")
            )
        }
    }

    override suspend fun read(
        eventType: String,
        range: ClosedRange<Instant>?,
        sourceDevice: Name?,
        targetDevice: Name?,
    ): List<DeviceMessage> = entityStore.computeInTransaction { transaction ->
        transaction.find(
            DEVICE_MESSAGE_ENTITY_TYPE,
            "type",
            eventType
        ).mapNotNull {
            if (it.timeInRange(range) &&
                it.propertyMatchesName(DeviceMessage::sourceDevice.name, sourceDevice) &&
                it.propertyMatchesName(DeviceMessage::targetDevice.name, targetDevice)
            ) {
                Json.decodeFromString(
                    DeviceMessage.serializer(),
                    it.getBlobString("json") ?: error("No json content found")
                )
            } else null
        }
    }

    override fun close() {
        entityStore.close()
    }

    public companion object : Factory<XodusDeviceMessageStorage> {
        internal const val DEVICE_MESSAGE_ENTITY_TYPE = "DeviceMessage"
        public val XODUS_STORE_PROPERTY: Name = Name.of("xodus", "storagePath")


        override fun invoke(meta: Meta, context: Context): XodusDeviceMessageStorage {
            val io = context.fetch(IOPlugin)
            val storePath = io.workDirectory.resolve(
                meta[XODUS_STORE_PROPERTY]?.string
                    ?: context.properties[XODUS_STORE_PROPERTY]?.string ?: "storage"
            )

            val entityStore = PersistentEntityStores.newInstance(storePath.toFile())

            return XodusDeviceMessageStorage(entityStore)
        }
    }
}

/**
 * Query all messages of given type
 */
@OptIn(ExperimentalSerializationApi::class)
public suspend inline fun <reified T : DeviceMessage> XodusDeviceMessageStorage.query(
    range: ClosedRange<Instant>? = null,
    sourceDevice: Name? = null,
    targetDevice: Name? = null,
): List<T> = read(serialDescriptor<T>().serialName, range, sourceDevice, targetDevice).map {
    //Check that all types are correct
    it as T
}
