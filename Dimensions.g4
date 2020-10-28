grammar Dimensions;

@parser::members {
    public static final DimensionsTable dimensionsTable = new DimensionsTable();
}

main: (stat ';')* EOF;

stat: dimension		#StatDimension
	| addUnit		#StatAddUnit
	;

dimension: 
	ID '=' value ID			#SimpleDimension
	| ID '=' expr (ID)?		#ComposeDimension
	;

value: 
	'int'		#IntValue
	| 'float'	#FloatValue
	;

addUnit: 'add' ID 'to' ID ':' expr;

expr:
	expr '^' expr						#PowExpr
	| expr op = ( '/'  | '*' ) expr		#MulDivExpr
	| expr op = ( '+'  | '-' ) expr		#AddSubExpr
	| '(' expr ')'						#ParenExpr
	| ID 								#IdExpr
	| NUMBER 							#NumberExpr
	;

NUMBER: INT | FLOAT;  
INT: [0-9]+;
FLOAT: INT '.' INT;

ID: [A-Za-z][A-Za-z_0-9]*;

COMMENT: '//' .*? '\n' -> skip;
MULTI_COMMENT: '/*' .*? '*/' -> skip;
WS: [ \r\n\t]+ -> skip;
