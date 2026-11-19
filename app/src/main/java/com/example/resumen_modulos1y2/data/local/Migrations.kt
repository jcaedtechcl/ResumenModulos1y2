package com.example.resumen_modulos1y2.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versión 1 → 2: se agrega la columna `archived`.
 *
 * Una migración dice EXACTAMENTE qué SQL transforma la base de la versión anterior en la nueva,
 * sin tocar las filas existentes. `DEFAULT 0` rellena las filas que ya estaban con "no
 * archivado" y debe coincidir con `@ColumnInfo(defaultValue = "0")` de la entidad: Room compara
 * el resultado con el esquema esperado y, si algo difiere, la app falla al abrir la base.
 *
 * Las migraciones no se borran nunca: un usuario que actualiza desde una versión muy antigua
 * las necesita todas, en cadena (1→2, 2→3, …).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reminders ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
    }
}
