import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.imageio.ImageIO;

public class ClienteTema07UDP {
    public static void main(String[] args) {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            // Altere para o IP da máquina do servidor no laboratório
            InetAddress IPAddress = InetAddress.getByName("localhost");
            
            // 1. Captura a imagem da tela (Tema 07)
            Robot robot = new Robot();
            Rectangle areaTela = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage printScreen = robot.createScreenCapture(areaTela);
            System.out.println("PrintScreen capturado.");
            
            // 2. Converte a imagem capturada para um array de bytes (formato JPG para ficar mais leve)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(printScreen, "jpg", baos);
            byte[] imagemBytes = baos.toByteArray();
            
            // 3. Fatiamento da imagem para envio via UDP
            int tamanhoPacote = 60000; // Tamanho seguro para pacotes UDP
            int totalPacotes = (int) Math.ceil((double) imagemBytes.length / tamanhoPacote);
            
            System.out.println("Enviando imagem fatiada em " + totalPacotes + " pacotes UDP...");
            
            for (int i = 0; i < totalPacotes; i++) {
                int inicio = i * tamanhoPacote;
                int fim = Math.min(imagemBytes.length, inicio + tamanhoPacote);
                int tamanhoAtual = fim - inicio;
                
                byte[] dadosPacote = new byte[tamanhoAtual];
                System.arraycopy(imagemBytes, inicio, dadosPacote, 0, tamanhoAtual);
                
                DatagramPacket sendPacket = new DatagramPacket(dadosPacote, dadosPacote.length, IPAddress, 9870);
                clientSocket.send(sendPacket);
                
                // Pequeno atraso (sleep) crucial: evita congestionar a rede e perder pacotes
                Thread.sleep(5);
            }
            
            // 4. Envia o pacote finalizador "fim" para o servidor saber que terminou
            byte[] fimBytes = "fim".getBytes();
            DatagramPacket fimPacket = new DatagramPacket(fimBytes, fimBytes.length, IPAddress, 9870);
            clientSocket.send(fimPacket);
            
            System.out.println("Envio finalizado com sucesso!");
            clientSocket.close();
            
        } catch (Exception e) {
            System.out.println("Erro no Cliente: " + e.getMessage());
        }
    }
}