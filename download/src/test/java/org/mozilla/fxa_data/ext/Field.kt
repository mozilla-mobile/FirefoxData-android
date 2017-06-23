/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.ext

import java.lang.reflect.Field

/** Sets the field to accessible for the duration of the given function. */
fun Field.withAccessible(withAccessibleFn: (field: Field) -> Unit) {
    isAccessible = true
    withAccessibleFn(this)
    isAccessible = false
}
