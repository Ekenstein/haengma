grammar Sgf;

@header {package com.github.ekenstein.sgf.parser;}

collection      : gameTree+;
gameTree        : '(' sequence gameTree* ')';
sequence        : node+;
node            : ';' prop*;
prop            : move
                | setup
                | nodeAnnotation
                | moveAnnotation
                | markup
                | root
                | gameInfo
                | timing
                | misc
                | privateProp
                ;
move            : 'B' (NONE|VALUE)   #blackMove
                | 'W' (NONE|VALUE)   #whiteMove
                | 'KO' NONE         #ko
                | 'MN' VALUE         #moveNumber
                ;
setup           : 'AB' VALUE+        #addBlack
                | 'AW' VALUE+        #addWhite
                | 'AE' VALUE+        #addEmpty
                | 'PL' VALUE         #playerToPlay
                ;
nodeAnnotation  : 'C' (NONE|VALUE)   #comment
                | 'DM' VALUE         #evenPosition
                | 'GB' VALUE         #goodForBlack
                | 'GW' VALUE         #goodForWhite
                | 'HO' VALUE         #hotspot
                | 'N' (NONE|VALUE)   #nodeName
                | 'UC' VALUE         #unclearPosition
                | 'V' VALUE          #value
                ;
moveAnnotation  : 'BM' VALUE         #badMove
                | 'DO' NONE         #doubtful
                | 'IT' NONE         #interesting
                | 'TE' VALUE         #tesuji
                ;
markup          : 'AR' VALUE+        #arrow
                | 'CR' VALUE+        #circle
                | 'DD' (NONE|VALUE+) #dimPoints
                | 'LB' VALUE+        #label
                | 'LN' VALUE+        #line
                | 'MA' VALUE+        #mark
                | 'SL' VALUE+        #selected
                | 'SQ' VALUE+        #square
                | 'TR' VALUE+        #triangle
                ;
root            : 'AP' VALUE         #application
                | 'CA' VALUE         #charset
                | 'FF' VALUE         #fileFormat
                | 'GM' VALUE         #game
                | 'ST' VALUE         #style
                | 'SZ' VALUE         #size
                ;
gameInfo        : 'AN' (NONE|VALUE)  #annotation
                | 'BR' (NONE|VALUE)  #blackRank
                | 'BT' (NONE|VALUE)  #blackTeam
                | 'CP' (NONE|VALUE)  #copyright
                | 'DT' VALUE         #date
                | 'EV' (NONE|VALUE)  #event
                | 'GN' (NONE|VALUE)  #gameName
                | 'GC' (NONE|VALUE)  #gameComment
                | 'ON' (NONE|VALUE)  #opening
                | 'OT' (NONE|VALUE)  #overtime
                | 'PB' (NONE|VALUE)  #playerBlack
                | 'PC' (NONE|VALUE)  #place
                | 'PW' (NONE|VALUE)  #playerWhite
                | 'RE' (NONE|VALUE)  #result
                | 'RO' (NONE|VALUE)  #round
                | 'RU' (NONE|VALUE)  #rules
                | 'SO' (NONE|VALUE)  #source
                | 'TM' VALUE         #timeLimit
                | 'US' (NONE|VALUE)  #user
                | 'WR' (NONE|VALUE)  #whiteRank
                | 'WT' (NONE|VALUE)  #whiteTeam
                | 'HA' VALUE         #handicap
                | 'KM' VALUE         #komi
                ;
timing          : 'BL' VALUE         #blackTimeLeft
                | 'OB' VALUE         #otStonesBlack
                | 'OW' VALUE         #otStonesWhite
                | 'WL' VALUE         #whiteTimeLeft
                ;
misc            : 'FG' (NONE|VALUE)  #figure
                | 'PM' VALUE         #printMoveMode
                | 'VW' (NONE|VALUE+) #view
                ;
privateProp     : PROP_IDENTIFIER (NONE | VALUE+);

WS: [ \n\r\t]+ -> skip;
PROP_IDENTIFIER : 'A'..'Z'+;
NONE            : L_BRACKET R_BRACKET;
VALUE           : L_BRACKET ('\\]'|.)*? R_BRACKET;

fragment L_BRACKET : '[';
fragment R_BRACKET : ']';
