package org.cm.podd.report.model

import android.util.Log

/**
 * Created by pphetra on 23/8/2018 AD.
 */
class TemplateEvaluator private constructor() {

    init {
        val tmplCode = "var tmpl = function (str, data) {\n" +
                "  var f = !/[^\\w\\-.:]/.test(str)\n" +
                "    ? (tmpl.cache[str] = tmpl.cache[str] || tmpl(str))\n" +
                "    : new Function( // eslint-disable-line no-new-func\n" +
                "      tmpl.arg + ',tmpl',\n" +
                "      'var _e=tmpl.encode' +\n" +
                "          tmpl.helper +\n" +
                "          \",_s='\" +\n" +
                "          str.replace(tmpl.regexp, tmpl.func) +\n" +
                "          \"';return _s;\"\n" +
                "    )\n" +
                "  return data\n" +
                "    ? f(data, tmpl)\n" +
                "    : function (data) {\n" +
                "      return f(data, tmpl)\n" +
                "    }\n" +
                "}\n" +
                "tmpl.cache = {}\n" +
                "tmpl.load = function (id) {\n" +
                "  return document.getElementById(id).innerHTML\n" +
                "}\n" +
                "tmpl.regexp = /([\\s'\\\\])(?!(?:[^{]|\\{(?!%))*%\\})|(?:\\{%(=|#)([\\s\\S]+?)%\\})|(\\{%)|(%\\})/g\n" +
                "tmpl.func = function (s, p1, p2, p3, p4, p5) {\n" +
                "  if (p1) {\n" +
                "    // whitespace, quote and backspace in HTML context\n" +
                "    return (\n" +
                "      {\n" +
                "        '\\n': '\\\\n',\n" +
                "        '\\r': '\\\\r',\n" +
                "        '\\t': '\\\\t',\n" +
                "        ' ': ' '\n" +
                "      }[p1] || '\\\\' + p1\n" +
                "    )\n" +
                "  }\n" +
                "  if (p2) {\n" +
                "    // interpolation: {%=prop%}, or unescaped: {%#prop%}\n" +
                "    if (p2 === '=') {\n" +
                "      return \"'+_e(\" + p3 + \")+'\"\n" +
                "    }\n" +
                "    return \"'+(\" + p3 + \"==null?'':\" + p3 + \")+'\"\n" +
                "  }\n" +
                "  if (p4) {\n" +
                "    // evaluation start tag: {%\n" +
                "    return \"';\"\n" +
                "  }\n" +
                "  if (p5) {\n" +
                "    // evaluation end tag: %}\n" +
                "    return \"_s+='\"\n" +
                "  }\n" +
                "}\n" +
                "tmpl.encReg = /[<>&\"'\\x00]/g // eslint-disable-line no-control-regex\n" +
                "tmpl.encMap = {\n" +
                "  '<': '&lt;',\n" +
                "  '>': '&gt;',\n" +
                "  '&': '&amp;',\n" +
                "  '\"': '&quot;',\n" +
                "  \"'\": '&#39;'\n" +
                "}\n" +
                "tmpl.encode = function (s) {\n" +
                "  return (s == null ? '' : '' + s).replace(tmpl.encReg, function (c) {\n" +
                "    return tmpl.encMap[c] || ''\n" +
                "  })\n" +
                "}\n" +
                "tmpl.arg = 'data'\n" +
                "tmpl.helper =\n" +
                "  \",print=function(s,e){_s+=e?(s==null?'':s):_e(s);}\" +\n" +
                "  ',include=function(s,d){_s+=tmpl(s,d);}'"
        engine.evaluateExpression(tmplCode)
    }

    private object Holder {
        val INSTANCE = TemplateEvaluator()
    }

    companion object {
        val instance: TemplateEvaluator by lazy { Holder.INSTANCE }
        val engine : ExpressionEngine = ExpressionEngine()
    }

    fun evaluate(template : String, jsonData : String) : String {
        // Add one space to the end of the template to
        // prevent recursive bug in tmpl script
        val script = "tmpl(\"${template} \", ${jsonData})"
        Log.d("debug", script)
        return engine.evaluateExpression(script).toString()
    }
}