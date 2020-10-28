grammar General;

@parser::members{
    public static final SymbolTable symbolTable = new SymbolTable();
}

main: (importDimensions)* statList EOF;

importDimensions: 'import' STRING TERM;

statList : (stat TERM)*;

stat: declaration | assign | print | conditional | loops;

declaration: type ID; 

type returns[Type res]:
	  'Integer'		#IntegerType			
	| 'Float'		#FloatType		
	| 'Boolean'		#BooleanType	
	| 'String'		#StringType	
	| ID 			#DimensionType
	;

assign: 
	declaration '=' expr	#DecAssign
	| ID '=' expr			#OnlyAssign
	;

print: 'print' expr;

expr returns[Type exprType, String varName, String dimension, String unit]:
	signal = ('+'|'-') expr												#Unary
	| expr '^' expr														#Pow
	| expr op = ('*' | '/') expr										#MultDiv
	| expr op = ('+' | '-') expr										#AddSub	
	| '!' expr 															#Not
	| expr op=('<' | '>' | '<=' | '>=' | '==' | '!=' ) expr 			#Condition
	| expr op=('||' | '&&') expr										#AndOr
	| '(' expr ')'														#Paren	
	//| 'input' STRING ID?												#InputValue
	| ID																#IDValue
	| STRING															#StringValue
	| BOOLEAN															#BooleanValue
	| INT ID? 															#IntValue	
	| FLOAT ID?															#FloatValue
	;

conditional: 'if' '(' expr ')' '{' statList '}' elseConditon?;  
  
elseConditon: 'else' '{' statList '}' ;

loops: whileLoop; //| forLoop;

whileLoop: 'while' expr '{' statList '}';

//forLoop: 'for' assign ',' expr ',' expr '{' statList '}'; 

STRING: '"' .*? '"';
BOOLEAN: 'true' | 'false';
 
INT: [0-9]+;
FLOAT: INT '.' INT;

ID: [A-Za-z][A-Za-z_/*0-9]*;

TERM: ';';

COMMENT: '//' .*? '\n' -> skip;
MULTI_COMMENT: '/*' .*? '*/' -> skip;
WS: [ \n\r\t]+ -> skip;
