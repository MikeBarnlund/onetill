package com.onetill.shared.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): OneTillDb {
    val driver = driverFactory.createDriver()
    return OneTillDb(driver)
}
