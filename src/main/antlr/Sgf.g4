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
move            : 'B' (NONE|TEXT)   #blackMove
                | 'W' (NONE|TEXT)   #whiteMove
                | 'KO' NONE         #ko
                | 'MN' TEXT         #moveNumber
                ;
setup           : 'AB' TEXT+        #addBlack
                | 'AW' TEXT+        #addWhite
                | 'AE' TEXT+        #addEmpty
                | 'PL' TEXT         #playerToPlay
                ;
nodeAnnotation  : 'C' TEXT          #comment
                | 'DM' TEXT         #evenPosition
                | 'GB' TEXT         #goodForBlack
                | 'GW' TEXT         #goodForWhite
                | 'HO' TEXT         #hotspot
                | 'N' TEXT          #nodeName
                | 'UC' TEXT         #unclearPosition
                | 'V' TEXT          #value
                ;
moveAnnotation  : 'BM' TEXT         #badMove
                | 'DO' NONE         #doubtful
                | 'IT' NONE         #interesting
                | 'TE' TEXT         #tesuji
                ;
markup          : 'AR' TEXT+        #arrow
                | 'CR' TEXT+        #circle
                | 'DD' (NONE|TEXT+) #dimPoints
                | 'LB' TEXT+        #label
                | 'LN' TEXT+        #line
                | 'MA' TEXT+        #mark
                | 'SL' TEXT+        #selected
                | 'SQ' TEXT+        #square
                | 'TR' TEXT+        #triangle
                ;
root            : 'AP' TEXT         #application
                | 'CA' TEXT         #charset
                | 'FF' TEXT         #fileFormat
                | 'GM' TEXT         #game
                | 'ST' TEXT         #style
                | 'SZ' TEXT         #size
                ;
gameInfo        : 'AN' TEXT         #annotation
                | 'BR' TEXT         #blackRank
                | 'BT' TEXT         #blackTeam
                | 'CP' TEXT         #copyright
                | 'DT' TEXT         #date
                | 'EV' TEXT         #event
                | 'GN' TEXT         #gameName
                | 'GC' TEXT         #gameComment
                | 'ON' TEXT         #opening
                | 'OT' TEXT         #overtime
                | 'PB' TEXT         #playerBlack
                | 'PC' TEXT         #place
                | 'PW' TEXT         #playerWhite
                | 'RE' TEXT         #result
                | 'RO' TEXT         #round
                | 'RU' TEXT         #rules
                | 'SO' TEXT         #source
                | 'TM' TEXT         #timeLimit
                | 'US' TEXT         #user
                | 'WR' TEXT         #whiteRank
                | 'WT' TEXT         #whiteTeam
                | 'HA' TEXT         #handicap
                | 'KM' TEXT         #komi
                ;
timing          : 'BL' TEXT         #blackTimeLeft
                | 'OB' TEXT         #otStonesBlack
                | 'OW' TEXT         #otStonesWhite
                | 'WL' TEXT         #whiteTimeLeft
                ;
misc            : 'FG' (NONE|TEXT)  #figure
                | 'PM' TEXT         #printMoveMode
                | 'VW' TEXT+        #view
                ;
privateProp     : PROP_IDENTIFIER (NONE | TEXT+);

PROP_IDENTIFIER : 'A'..'Z' 'A'..'Z'+;
NONE            : L_BRACKET R_BRACKET;
TEXT            : L_BRACKET ('\\]'|.)*? R_BRACKET;

fragment L_BRACKET : '[';
fragment R_BRACKET : ']';

WS: [ \n\r\t]+ -> skip;