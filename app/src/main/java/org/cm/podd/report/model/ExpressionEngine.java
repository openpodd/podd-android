/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cm.podd.report.model;

import android.util.Log;

import org.cm.podd.report.model.rhino.ScriptableList;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.List;

/**
 * Created by pphetra on 9/26/14 AD.
 */
public class ExpressionEngine {

    private final Context cx;
    private final Scriptable scope;

    public ExpressionEngine() {
        cx = Context.enter();
        cx.setOptimizationLevel(-1);
        scope = cx.initStandardObjects();
        try {
            ScriptableList.init(scope);
        } catch (NoSuchMethodException e) {
            Log.d(ExpressionEngine.class.getName(), e.getMessage());
        }

    }

    public void destroy() {
        cx.exit();
    }

    public boolean evaluateBooleanExpression(String expression) {
        Object result = evaluateExpression(expression);
        Log.d("engine", String.format("%s -> %s", expression, result.toString()));
        if (result != null) {
            return (Boolean) result;
        }
        return false;
    }

    public Object evaluateExpression(String expression) {
        return cx.evaluateString(scope, expression, "source", 1, null);
    }

    public void setInteger(String name, int value) {
        scope.put(name, scope, new Integer(value));
    }

    public void setBoolean(String name, boolean value) {
        scope.put(name, scope, new Boolean(value));
    }

    public void setString(String name, String value) {
        scope.put(name, scope, value);
    }

    public void setStringArray(String name, List<String> values) {
        scope.put(name, scope, values);
    }

    public void setObject(String name, Object value) {
        scope.put(name, scope, value);
    }
}
