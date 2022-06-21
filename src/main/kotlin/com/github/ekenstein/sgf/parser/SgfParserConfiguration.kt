package com.github.ekenstein.sgf.parser

class SgfParserConfiguration {
    /**
     * Whether unknown properties should be stored or not.
     * If true, unknown properties will be stored as private properties, otherwise ignored.
     * Default is true.
     */
    var preserveUnknownProperties = true

    /**
     * Whether malformed properties should be ignored or not. Default is false.
     */
    var ignoreMalformedProperties = false
}
