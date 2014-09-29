/*
 *  Copyright 2006 Hannes Wallnoefer <hannes@helma.at>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cm.podd.report.model.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

import java.util.List;
import java.util.Map;

/**
 * A collection of Rhino utility methods.
 */
public class ScriptUtils {
    /**
     * Coerce/wrap a java object to a JS object, and mask Lists and Maps
     * as native JS objects.
     * @param obj the object to coerce/wrap
     * @param scope the scope
     * @return the wrapped/masked java object
     */
    @SuppressWarnings("unchecked")
    public static Object javaToJS(Object obj, Scriptable scope) {
        if (obj instanceof Scriptable) {
            if (obj instanceof ScriptableObject
                    && ((Scriptable) obj).getParentScope() == null
                    && ((Scriptable) obj).getPrototype() == null) {
                ScriptRuntime.setObjectProtoAndParent((ScriptableObject) obj, scope);
            }
            return obj;
        } else if (obj instanceof List) {
            return new ScriptableList(scope, (List) obj);
//        } else if (obj instanceof Map) {
//            return new ScriptableMap(scope, (Map) obj);
        } else {
            return Context.javaToJS(obj, scope);
        }
    }

    /**
     * Unwrap a JS object to a java object. This is much more conservative than
     * Context.jsToJava in that it will preserve undefined values.
     * @param obj a JavaScript value
     * @return a Java object corresponding to obj
     */
    public static Object jsToJava(Object obj) {
        while (obj instanceof Wrapper) {
            obj = ((Wrapper) obj).unwrap();
        }
        return obj;
    }
}
