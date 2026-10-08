package me.shadow.eclipselauncher.feature.download

import me.shadow.eclipselauncher.feature.download.enums.Category
import me.shadow.eclipselauncher.feature.download.enums.ModLoader
import me.shadow.eclipselauncher.feature.download.enums.Sort

/**
 * Provides filter information when searching on a platform
 */
class Filters {
    var name: String = ""
    var mcVersion: String? = null
    var modloader: ModLoader? = null
    var sort: Sort = Sort.RELEVANT
    var category: Category = Category.ALL

    override fun toString(): String {
        return "Filters(name='$name', mcVersion=$mcVersion, modloader=$modloader, sort=$sort, category=$category)"
    }
}