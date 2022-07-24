grammar Sgf;

@header {package com.github.ekenstein.sgf.parser;}

collection      : gameTree+;
gameTree        : '(' sequence gameTree* ')';
sequence        : node+;
node            : ';' prop*;
prop            : PROP_IDENTIFIER (NONE | VALUE+);

WS: [ \n\r\t]+ -> skip;
PROP_IDENTIFIER : 'A'..'Z'+;
NONE            : L_BRACKET R_BRACKET;
VALUE           : L_BRACKET ('\\\\'|'\\]'|.)*? R_BRACKET;

fragment L_BRACKET : '[';
fragment R_BRACKET : ']';
