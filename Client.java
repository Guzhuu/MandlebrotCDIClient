import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class Client{
	public static void main(String[] args){
		int nClientes = 4;
		int peticiones = 300;
		int puerto = 4444;
		try{
			nClientes = Integer.parseInt(args[0]);
		}catch(Exception e){
			nClientes = 4;
			System.out.println("Nº clientes no válido, se pondrá a 4");
		}
		try {
			puerto = Integer.parseInt(args[1]);
		}catch(Exception e) {
			System.out.println("No se ha especificado nº de puerto, se pondrá 4444");
			puerto = 4444;
		}
		try {
			peticiones = Integer.parseInt(args[2]);
		}catch(Exception e) {
			System.out.println("No se ha especificado nº de peticiones por cliente, o es inválido, se pondrá a 300");
			peticiones = 300;
		}
		for(int i = 0; i < nClientes; i++){
			new Cliente(puerto, peticiones).start();
		}
	}
}

class Cliente extends Thread{
	int ID;
	static int nClientes = 0;
	InetAddress host = null;
	Socket socket = null;
	ObjectOutputStream oos = null;
	ObjectInputStream ois = null;
	Integer puerto;
	int peticiones;
	
	public Cliente(Integer puerto, int peticiones) {
		this.peticiones = peticiones;
		this.puerto = puerto;
		this.ID = ++nClientes;
	}
	
	public int[][] filtrar(RegionYMsg region){
		int[][] retorno = region.getRegion();
		double coorX = 0.0;
		double coorY = 0.0;
		double aux = 0.0;
		double z1 = 0.0;
		double z2 = 0.0;
		int iteraciones = 0;
		
		for(int x = 0; x < retorno.length; x++){
			for(int y = 0; y < retorno[0].length; y++){
				coorX = region.getCoordenadaX(region.getX() * retorno.length + x);
				coorY = region.getCoordenadaY(region.getY() * retorno[0].length + y);
				iteraciones = region.getIteraciones();
				z1 = 0.0;
				z2 = 0.0;
				aux = 0.0;
				
				
				while(z1*z1+z2*z2 < 4 && iteraciones > 0){
					aux = z1*z1 - z2*z2 + coorX;
					z2 = 2*z1*z2 + coorY;
					z1 = aux;
					iteraciones--;
				}
				if(region.getModo() == 'g'){
					retorno[x][y] = (511/(region.getIteraciones()%512))*(iteraciones%512);
				}else if(region.getModo() == 'h'){
					retorno[x][y] = (65535/(region.getIteraciones()%65536))*(iteraciones%65536);
				}else{
					retorno[x][y] = (16777215/(region.getIteraciones()%16777216))*(iteraciones%16777216);
				}
			}
		}
		return retorno;
	}
	
	public void run() {
		try {
			for(int x=0; x<peticiones; x++) {
				host = InetAddress.getLocalHost();
				socket = new Socket(host.getHostName(), puerto);
				
				System.out.println("[CLIENTE " + this.ID + "]: Enviando petición de zona");
				oos = new ObjectOutputStream(socket.getOutputStream());
				oos.writeObject(new RegionYMsg("Oiga usté, deme una nueva región"));
				
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				RegionYMsg recibido = (RegionYMsg) ois.readObject();
				System.out.println("[CLIENTE " + this.ID + "]: Recibido algo");
				if(recibido.getMsg().equals("Hijo mio, no te puedo enviar una region") || recibido.getMsg().equals("Hijo mio, no te puedo enviar una region, ya están todas usadas")) {
					System.out.println("[CLIENTE " + this.ID + "]: " + recibido.getMsg());
				}else if(recibido.getMsg().equals("Region a filtrar")){
					System.out.println("[CLIENTE " + this.ID + "]: Recibida una región, procediendo a filtrado de la misma");
					
					recibido.setRegion(filtrar(recibido));
					recibido.setMsg("Region filtrada");
					oos.writeObject(recibido);
					System.out.println("[CLIENTE " + this.ID + "]: Región filtrada enviada");
				}else if(recibido.getMsg().equals("Error")){
					System.out.println("[CLIENTE " + this.ID + "]: Recibido un error");
					
					//oos.writeObject(new RegionYMsg("Error"));
				}
				
				ois.close();
				oos.close();
				socket.close();
			}
			System.out.println("[CLIENTE " + this.ID + "]: Fin de peticiones");
		} catch (Exception e) {
			System.out.println("En el cliente" + this.ID + ": " + e.toString());
			try{
				oos.writeObject(new RegionYMsg("Error"));
				ois.close();
				oos.close();
				socket.close();
			}catch(Exception eh){
				System.out.println("En el cliente, excepción de excepciones" + this.ID + ": " + eh.toString());
			}
		}
	}
}