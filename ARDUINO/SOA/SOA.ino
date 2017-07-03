
#include <OneWire.h>                  //Se importan las librerías
#include <DallasTemperature.h>
#define Pin 2                         //Se declara el pin donde se conectará la DATA
OneWire ourWire(Pin);                //Se establece el pin declarado como bus para la comunicación OneWire
DallasTemperature sensors(&ourWire); //Se llama a la librería DallasTemperature

/////// PINES SALIDAS///////
int ledPuerta = 9;   //DIGITAL
int bombaAgua = 11; // RELE1
int luz       = 13; // DIGITAL
int cooler    = 12; // DIGITAL

/////// PINES ENTRADAS /////  
const int puerta = 8; 
char inbyte = 0; //Char para leer el led
char inbyteAnterior;
float celsius;
int cantLuz;
int humedad;
const int sensorHumedad = A1;
const int sensorLuz = A2;
int estado_cooler = LOW ;
int estado_luz = LOW;
int estado_bomba = LOW;
boolean automatico = true;
boolean riegoManual = false;

/////// VARIABLES DE ENTORNO //////////
long tiempoInicioRegado;
boolean esDeDia;
int cantMuestras=0;
int maxMuestras=10;
int muestraAbierta=0;
int muestraCerrada=0;
boolean puertaAbierta;
int HUMEDAD_PARAMETRO = 850;
int TEMPERATURA_PARAMETRO = 25;
////// VECTOR DE ENVIO DE INFORMACION //////////
//  [0] RELE VALVULA ESTSADO (APAGADO PRENDIDO)
//  [1] RELE LUZ ESTADO(APAGADO PRENDIDO)
//  [2] COOLER(APAGADO PRENDIDO)
//  [3] TEMPERATURA(CELSIUS)
//  [4] HUMEDAD(DATO)
float valorVoltaje[5] = {0, 0, 0, 0, 0};

void setup() {

  pinMode(ledPuerta, OUTPUT);
  pinMode(cooler, OUTPUT);
  pinMode(bombaAgua, OUTPUT);
  pinMode(luz, OUTPUT);
  pinMode(puerta,INPUT);
  digitalWrite(bombaAgua, HIGH); //Activo el rele de la bomba
  digitalWrite(cooler, LOW); //Activo el rele de la bomba
  digitalWrite(luz, HIGH); //Activo el rele de la bomba
  Serial.begin(9600);
  sensors.begin();
}

void loop() {

  leerBT();

  // BLOCO AUTOMATICO
  if (automatico && !riegoManual){
    controlPuerta();
    controlDeTemperatura(cooler);
    controlDeHumedadTierra();
    controlLuz();
  }

  if( riegoManual == true ){
    if( (millis() - tiempoInicioRegado ) < 5000 ){
         digitalWrite(bombaAgua, LOW); //Activo el rele de la bomba
         estado_bomba = HIGH;
    }else{
      riegoManual = false;
      digitalWrite(bombaAgua, HIGH); //Activo el rele de la bomba
      estado_bomba = LOW;
    }
  }
  getValorVoltaje();
  sendValoresAndroid();

}


///////////////////////////////////////////////////////////////////////////////////////////
// FUNCION QUE SE ENCARGA DE TOMAR LOS VALORES DE BT Y SETEAR LOS RESPECTIVOS COKPONENTES /
///////////////////////////////////////////////////////////////////////////////////////////
void leerBT() {
  if (Serial.available() > 0)
  {
    // LEEMOS COMANDO
    inbyte = Serial.read();
    Serial.println(inbyte);
    // PARAMETRIZAMOS LA TEMPERATURA
    if( inbyte == 't'){
      delay(10);
      String val = "";
       while(Serial.available() > 0){
          inbyte = Serial.read();
          val = val + inbyte;
          delay(10);
       }
       TEMPERATURA_PARAMETRO = val.toInt();
    }

    // PARAMETRIZAMOS LA HUMEDAD
    if( inbyte == 'h'){
      delay(10);
      String val = "";
       while(Serial.available() > 0){
          inbyte = Serial.read();
          val = val + inbyte;
          delay(10);
       }
       HUMEDAD_PARAMETRO = val.toInt();
    }
    
    // APAGAMOS EL LED CON EL BOTON
    if (inbyte == '1'){
      digitalWrite(luz, LOW); //LED arduino OFF
      estado_luz = HIGH;
      //valorVoltaje[1] = 1;
    }
    
    // APAGAMOS EL LED CON EL BOTON
    if (inbyte == '2'){
      digitalWrite(luz, HIGH); //LED arduino ON
      estado_luz = LOW;
      //valorVoltaje[1] = 1;
    }
    if (inbyte == '8'){
      digitalWrite(luz, LOW); //LED arduino OFF
      estado_luz = HIGH;
      //valorVoltaje[1] = 1;
    }
    
    // APAGAMOS EL LED CON EL BOTON
    if (inbyte == '9'){
      digitalWrite(luz, HIGH); //LED arduino ON
      estado_luz = LOW;
      //valorVoltaje[1] = 1;
    }
    
    // PRENDEMOS EL COOLER
    if (inbyte == '4' ){
      digitalWrite(cooler, HIGH); //Cooler On
      estado_cooler = LOW;
      
    // APAGAMOS EL COOLER
    }
    if (inbyte == '5' ){
      digitalWrite(cooler, LOW); //Cooler Off
      estado_cooler = HIGH;
    }
    
    // PASAMOS A AUTOMATICO
    if (inbyte == 'A' ){
       automatico = true;
    }
    
    // PASAMOS A MANUAL
        if (inbyte == 'M' ){
       automatico = false;
    }

    // REGAMOS
    if (inbyte == 'R' ){
      riegoManual = true;
      tiempoInicioRegado = millis();
    }

    
    inbyteAnterior = inbyte;
    inbyte='X';
     delay(20);
  }
}


////////////////////////////////////////////////////////////////
// FUNCION QUE SE ENCARGA DE ESTABLECER EL ESTADO DE LA PUERTA /
////////////////////////////////////////////////////////////////
void controlPuerta() {
  int estado = digitalRead(puerta);
  if( estado == HIGH ){
    //puertaAbierta=true;
    //Serial.println(estado);
    muestraAbierta++;
  }else{
    //puertaAbierta=false;
    muestraCerrada++;
    //Serial.println(estado);
  }
  cantMuestras++;

  if( cantMuestras == maxMuestras ){
    cantMuestras=0;
    if (muestraAbierta-3 > muestraCerrada){
      puertaAbierta=true;
      digitalWrite(ledPuerta, HIGH);
      //Serial.println("ABIERTO");
    }else{
      puertaAbierta=false;
       digitalWrite(ledPuerta, LOW);
      //Serial.println("CERRADO");
    }
    muestraAbierta=0;
    muestraCerrada=0;
  }
}


////////////////////////////////////////////
// FUNCION QUE SE ENCARGA DE CONTROLAR LUZ /
////////////////////////////////////////////
void controlLuz() {
  
    cantLuz = analogRead(sensorLuz);
   
    if ( cantLuz > 60 && !puertaAbierta ){
    // ES DE DIA
      esDeDia = true;
      digitalWrite(luz, LOW); //LAMPARA OFF
      estado_luz = HIGH;
    }else{
    // ES DE NOCHE
      esDeDia = false;
      digitalWrite(luz, HIGH); //LED arduino OFF
      estado_luz = LOW;
    }
}

///////////////////////////////////////////////////////
// FUNCION QUE SE ENCARGA DE CONTROLAR LA TEMPERATURA /
///////////////////////////////////////////////////////

void controlDeTemperatura(int cooler) {

  sensors.requestTemperatures();       //Prepara el sensor para la lectura

  celsius = sensors.getTempCByIndex(0);
  //Serial.print(celsius); //Se lee e imprime la temperatura en grados Centigrados
  //Serial.println(" Grados Centigrados");
  if (celsius > TEMPERATURA_PARAMETRO)
  {
    estado_cooler = HIGH;
  }
  else
  {
    estado_cooler = LOW;
  }
  digitalWrite(cooler, estado_cooler);

}


////////////////////////////////////////////////////
// FUNCION QUE SE ENCARGA DE CONTROLAR LA HUMESDAD /
////////////////////////////////////////////////////
void controlDeHumedadTierra()
  {

  humedad = analogRead(sensorHumedad);
  //Serial.println(humedad);
  //Serial.println ("");
  if (humedad > HUMEDAD_PARAMETRO && !esDeDia){
    digitalWrite(bombaAgua, LOW); //Activo el rele de la bomba
    estado_bomba = HIGH;
  }
  else{  
    digitalWrite(bombaAgua, HIGH); // Desactivo el rele de la bomba
    estado_bomba = LOW;
  }
  //digitalWrite(bombaAgua, HIGH);


}

///////////////////////////////////////////////////////////////
// FUNCION QUE SE ENCARGA DE LLENAR LOS DATOS DE COMUNICACION /
///////////////////////////////////////////////////////////////
void getValorVoltaje()
{
  if ( estado_bomba == HIGH) {
    valorVoltaje[0] = 1;
  }
  else {
    valorVoltaje[0] = 0;
  }

  if (estado_luz == HIGH) {
    valorVoltaje[1] = 1;
  }
  else {
    valorVoltaje[1] = 0;
  }

  if (estado_cooler == HIGH) {
    valorVoltaje[2] = 1;
  }
  else {
    valorVoltaje[2] = 0;
  }

  valorVoltaje[3] = celsius;
  valorVoltaje[4] = humedad;
}

///////////////////////////////////////////////////////////////
// FUNCION QUE SE ENCARGA DE ENVIAR LOS DATOS DE COMUNICACION /
///////////////////////////////////////////////////////////////
void sendValoresAndroid()
{
  
  String linea;
  linea = '#';
  // Caracter que indica el comienzo de transmision
  for (int k = 0; k < 5; k++)
  {
    linea = linea + (valorVoltaje[k]);
    linea = linea + ';'; //separador
  }
  linea = linea+ '~'; //Fin de transmision
  
  Serial.println(linea);
  delay(50);        //eliminamos transmisiones faltantes de procesar
}



