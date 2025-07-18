// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.java.workspace.entities.impl

import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.openapi.util.NlsSafe
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.impl.EntityLink
import com.intellij.platform.workspace.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.impl.extractOneToManyParent
import com.intellij.platform.workspace.storage.impl.updateOneToManyParentOfChild
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.instrumentation.MutableEntityStorageInstrumentation
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata

@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(7)
@OptIn(WorkspaceEntityInternalApi::class)
internal class JavaSourceRootPropertiesEntityImpl(private val dataSource: JavaSourceRootPropertiesEntityData) :
  JavaSourceRootPropertiesEntity, WorkspaceEntityBase(dataSource) {

  private companion object {
    internal val SOURCEROOT_CONNECTION_ID: ConnectionId = ConnectionId.create(
      SourceRootEntity::class.java, JavaSourceRootPropertiesEntity::class.java, ConnectionId.ConnectionType.ONE_TO_MANY, false
    )

    private val connections = listOf<ConnectionId>(
      SOURCEROOT_CONNECTION_ID,
    )

  }

  override val sourceRoot: SourceRootEntity
    get() = snapshot.extractOneToManyParent(SOURCEROOT_CONNECTION_ID, this)!!

  override val generated: Boolean
    get() {
      readField("generated")
      return dataSource.generated
    }
  override val packagePrefix: String
    get() {
      readField("packagePrefix")
      return dataSource.packagePrefix
    }

  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }


  internal class Builder(result: JavaSourceRootPropertiesEntityData?) :
    ModifiableWorkspaceEntityBase<JavaSourceRootPropertiesEntity, JavaSourceRootPropertiesEntityData>(result),
    JavaSourceRootPropertiesEntity.Builder {
    internal constructor() : this(JavaSourceRootPropertiesEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity JavaSourceRootPropertiesEntity is already created in a different builder")
        }
      }

      this.diff = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()
      // After adding entity data to the builder, we need to unbind it and move the control over entity data to builder
      // Builder may switch to snapshot at any moment and lock entity data to modification
      this.currentEntityData = null

      // Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization() // TODO uncomment and check failed tests
    }

    private fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (_diff != null) {
        if (_diff.extractOneToManyParent<WorkspaceEntityBase>(SOURCEROOT_CONNECTION_ID, this) == null) {
          error("Field JavaSourceRootPropertiesEntity#sourceRoot should be initialized")
        }
      }
      else {
        if (this.entityLinks[EntityLink(false, SOURCEROOT_CONNECTION_ID)] == null) {
          error("Field JavaSourceRootPropertiesEntity#sourceRoot should be initialized")
        }
      }
      if (!getEntityData().isPackagePrefixInitialized()) {
        error("Field JavaSourceRootPropertiesEntity#packagePrefix should be initialized")
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as JavaSourceRootPropertiesEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.generated != dataSource.generated) this.generated = dataSource.generated
      if (this.packagePrefix != dataSource.packagePrefix) this.packagePrefix = dataSource.packagePrefix
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }

    override var sourceRoot: SourceRootEntity.Builder
      get() {
        val _diff = diff
        return if (_diff != null) {
          @OptIn(EntityStorageInstrumentationApi::class)
          ((_diff as MutableEntityStorageInstrumentation).getParentBuilder(SOURCEROOT_CONNECTION_ID, this) as? SourceRootEntity.Builder)
          ?: (this.entityLinks[EntityLink(false, SOURCEROOT_CONNECTION_ID)]!! as SourceRootEntity.Builder)
        }
        else {
          this.entityLinks[EntityLink(false, SOURCEROOT_CONNECTION_ID)]!! as SourceRootEntity.Builder
        }
      }
      set(value) {
        checkModificationAllowed()
        val _diff = diff
        if (_diff != null && value is ModifiableWorkspaceEntityBase<*, *> && value.diff == null) {
          // Setting backref of the list
          if (value is ModifiableWorkspaceEntityBase<*, *>) {
            val data = (value.entityLinks[EntityLink(true, SOURCEROOT_CONNECTION_ID)] as? List<Any> ?: emptyList()) + this
            value.entityLinks[EntityLink(true, SOURCEROOT_CONNECTION_ID)] = data
          }
          // else you're attaching a new entity to an existing entity that is not modifiable
          _diff.addEntity(value as ModifiableWorkspaceEntityBase<WorkspaceEntity, *>)
        }
        if (_diff != null && (value !is ModifiableWorkspaceEntityBase<*, *> || value.diff != null)) {
          _diff.updateOneToManyParentOfChild(SOURCEROOT_CONNECTION_ID, this, value)
        }
        else {
          // Setting backref of the list
          if (value is ModifiableWorkspaceEntityBase<*, *>) {
            val data = (value.entityLinks[EntityLink(true, SOURCEROOT_CONNECTION_ID)] as? List<Any> ?: emptyList()) + this
            value.entityLinks[EntityLink(true, SOURCEROOT_CONNECTION_ID)] = data
          }
          // else you're attaching a new entity to an existing entity that is not modifiable

          this.entityLinks[EntityLink(false, SOURCEROOT_CONNECTION_ID)] = value
        }
        changedProperty.add("sourceRoot")
      }

    override var generated: Boolean
      get() = getEntityData().generated
      set(value) {
        checkModificationAllowed()
        getEntityData(true).generated = value
        changedProperty.add("generated")
      }

    override var packagePrefix: String
      get() = getEntityData().packagePrefix
      set(value) {
        checkModificationAllowed()
        getEntityData(true).packagePrefix = value
        changedProperty.add("packagePrefix")
      }

    override fun getEntityClass(): Class<JavaSourceRootPropertiesEntity> = JavaSourceRootPropertiesEntity::class.java
  }
}

@OptIn(WorkspaceEntityInternalApi::class)
internal class JavaSourceRootPropertiesEntityData : WorkspaceEntityData<JavaSourceRootPropertiesEntity>() {
  var generated: Boolean = false
  lateinit var packagePrefix: String


  internal fun isPackagePrefixInitialized(): Boolean = ::packagePrefix.isInitialized

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntity.Builder<JavaSourceRootPropertiesEntity> {
    val modifiable = JavaSourceRootPropertiesEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  @OptIn(EntityStorageInstrumentationApi::class)
  override fun createEntity(snapshot: EntityStorageInstrumentation): JavaSourceRootPropertiesEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = JavaSourceRootPropertiesEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn("com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity") as EntityMetadata
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return JavaSourceRootPropertiesEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntity.Builder<*>>): WorkspaceEntity.Builder<*> {
    return JavaSourceRootPropertiesEntity(generated, packagePrefix, entitySource) {
      parents.filterIsInstance<SourceRootEntity.Builder>().singleOrNull()?.let { this.sourceRoot = it }
    }
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    res.add(SourceRootEntity::class.java)
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as JavaSourceRootPropertiesEntityData

    if (this.entitySource != other.entitySource) return false
    if (this.generated != other.generated) return false
    if (this.packagePrefix != other.packagePrefix) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as JavaSourceRootPropertiesEntityData

    if (this.generated != other.generated) return false
    if (this.packagePrefix != other.packagePrefix) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + generated.hashCode()
    result = 31 * result + packagePrefix.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + generated.hashCode()
    result = 31 * result + packagePrefix.hashCode()
    return result
  }
}
