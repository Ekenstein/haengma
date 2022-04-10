grammar Sgf;

@header {package com.github.ekenstein.sgf.parser;}

collection      : gameTree+;
gameTree        : '(' sequence gameTree* ')';
sequence        : node+;
node            : ';' prop+;
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
nodeAnnotation  : 'C' VALUE          #comment
                | 'DM' VALUE         #evenPosition
                | 'GB' VALUE         #goodForBlack
                | 'GW' VALUE         #goodForWhite
                | 'HO' VALUE         #hotspot
                | 'N' VALUE          #nodeName
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
gameInfo        : 'AN' VALUE         #annotation
                | 'BR' VALUE         #blackRank
                | 'BT' VALUE         #blackTeam
                | 'CP' VALUE         #copyright
                | 'DT' VALUE         #date
                | 'EV' VALUE         #event
                | 'GN' VALUE         #gameName
                | 'GC' VALUE         #gameComment
                | 'ON' VALUE         #opening
                | 'OT' VALUE         #overtime
                | 'PB' VALUE         #playerBlack
                | 'PC' VALUE         #place
                | 'PW' VALUE         #playerWhite
                | 'RE' VALUE         #result
                | 'RO' VALUE         #round
                | 'RU' VALUE         #rules
                | 'SO' VALUE         #source
                | 'TM' VALUE         #timeLimit
                | 'US' VALUE         #user
                | 'WR' VALUE         #whiteRank
                | 'WT' VALUE         #whiteTeam
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
PROP_IDENTIFIER : 'A'..'Z' 'A'..'Z'+;
NONE            : L_BRACKET R_BRACKET;
VALUE           : L_BRACKET ('\\]'|.)*? R_BRACKET;

fragment L_BRACKET : '[';
fragment R_BRACKET : ']';
