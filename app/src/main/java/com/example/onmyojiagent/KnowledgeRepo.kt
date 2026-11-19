package com.example.onmyojiagent

import android.database.sqlite.SQLiteDatabase

/** 页面指纹：must 全含 + any 任一，双规则防文案微调导致整页误判 */
data class PageFingerprint(
    val mustText: List<String>,
    val anyText: List<String>
) {
    fun matches(allText: Set<String>): Boolean {
        if (mustText.any { it !in allText }) return false
        if (anyText.isNotEmpty() && anyText.none { it in allText }) return false
        return true
    }
}

data class UiElement(
    val elementId: String,
    val resourceId: String?,
    val text: String?,
    val region: String?,   // "relX,relY" 相对中心坐标
    val status: String?
)

/**
 * 领域知识库（SQLite）：版本对齐 + 页面指纹 + 元素词典。
 * MVP 先落地"版本不一致→暂停"、"页面指纹命中"、"元素词典定位"。
 */
object KnowledgeRepo {
    private lateinit var db: SQLiteDatabase

    fun init(path: String) {
        db = SQLiteDatabase.openOrCreateDatabase(path, null)
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS game_version(
                 version_code TEXT PRIMARY KEY, valid INTEGER DEFAULT 1)"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS page(
                 page_id TEXT PRIMARY KEY, must_text TEXT, any_text TEXT, back_action TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS ui_element(
                 element_id TEXT PRIMARY KEY, page_id TEXT, resource_id TEXT,
                 text TEXT, region TEXT, version_range TEXT, status TEXT)"""
        )
    }

    /** 冷启动种子：示例页面与元素（后续由采集/标注填充） */
    fun seedDefaults() {
        insertPage("p_home", must = "首页", any = null, backAction = null)
        insertPage("p_signin", must = "签到", any = "领取,今日", backAction = "close_x")
        insertElement("e_signin_claim", "p_signin", null, "领取", null)
        insertElement("e_signin_close", "p_signin", null, null, "0.92,0.06")  // 右上角 X 区域
    }

    fun insertPage(pageId: String, must: String?, any: String?, backAction: String?) {
        db.execSQL(
            "INSERT OR REPLACE INTO page(page_id, must_text, any_text, back_action) VALUES(?,?,?,?)",
            arrayOf(pageId, must, any, backAction)
        )
    }

    fun insertElement(
        elementId: String, pageId: String?, resourceId: String?, text: String?, region: String?
    ) {
        db.execSQL(
            "INSERT OR REPLACE INTO ui_element(element_id, page_id, resource_id, text, region, version_range, status) VALUES(?,?,?,?,?,?,?)",
            arrayOf(elementId, pageId, resourceId, text, region, null, "active")
        )
    }

    /** 当前游戏版本是否与知识库对齐；不对齐 → 返回 false，上层暂停并提示更新知识库 */
    fun isVersionAligned(currentVersion: String): Boolean {
        val c = db.rawQuery(
            "SELECT valid FROM game_version WHERE version_code=?",
            arrayOf(currentVersion)
        )
        val ok = c.moveToFirst() && c.getInt(0) == 1
        c.close()
        return ok
    }

    fun pageFingerprint(pageId: String): PageFingerprint? {
        val c = db.rawQuery(
            "SELECT must_text, any_text FROM page WHERE page_id=?",
            arrayOf(pageId)
        )
        if (!c.moveToFirst()) { c.close(); return null }
        val must = c.getString(0).orEmpty().split(",").filter { it.isNotBlank() }
        val any = c.getString(1).orEmpty().split(",").filter { it.isNotBlank() }
        c.close()
        return PageFingerprint(must, any)
    }

    fun backAction(pageId: String): String? {
        val c = db.rawQuery("SELECT back_action FROM page WHERE page_id=?", arrayOf(pageId))
        val r = if (c.moveToFirst()) c.getString(0) else null
        c.close()
        return r
    }

    fun element(elementId: String): UiElement? {
        val c = db.rawQuery(
            "SELECT resource_id, text, region, status FROM ui_element WHERE element_id=?",
            arrayOf(elementId)
        )
        if (!c.moveToFirst()) { c.close(); return null }
        val e = UiElement(elementId, c.getString(0), c.getString(1), c.getString(2), c.getString(3))
        c.close()
        return e
    }
}
