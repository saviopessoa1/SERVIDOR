import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

public class ClienteTema07UDP {
    public static void main(String[] args) {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            // Altere para o IP da máquina do servidor no laboratório
            InetAddress IPAddress = InetAddress.getByName("localhost");
            
            // 1. Captura a imagem da tela
            Robot robot = new Robot();
            Rectangle areaTela = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage printScreen = robot.createScreenCapture(areaTela);
            System.out.println("PrintScreen capturado.");
            
            // 2. Converte a imagem capturada para um array de bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(printScreen, "jpg", baos);
            byte[] imagemBytes = baos.toByteArray();
            
            // 3. Fatiamento da imagem em pacotes de 1024 bytes (Conforme roteiro)
            int tamanhoPayload = 1024; 
            int totalPacotes = (int) Math.ceil((double) imagemBytes.length / tamanhoPayload);
            
            System.out.println("Enviando imagem fatiada em " + totalPacotes + " pacotes UDP...");
            
            // --- PACOTE INIT ---
            // Primeiro byte 'I' seguido do tamanho total da imagem
            ByteBuffer initBuffer = ByteBuffer.allocate(5); 
            initBuffer.put((byte) 'I');
            initBuffer.putInt(imagemBytes.length);
            byte[] initBytes = initBuffer.array();
            clientSocket.send(new DatagramPacket(initBytes, initBytes.length, IPAddress, 9870));
            
            Thread.sleep(50); // Breve pausa para o servidor inicializar o buffer local
            
            // --- PACOTES DATA ---
            for (int i = 0; i < totalPacotes; i++) {
                int inicio = i * tamanhoPayload;
                int fim = Math.min(imagemBytes.length, inicio + tamanhoPayload);
                int tamanhoAtual = fim - inicio;
                
                // Primeiro byte 'D' + 4 bytes de sequencia + dados da imagem
                ByteBuffer dataBuffer = ByteBuffer.allocate(1 + 4 + tamanhoAtual);
                dataBuffer.put((byte) 'D');
                dataBuffer.putInt(i); // Número de sequência
                dataBuffer.put(imagemBytes, inicio, tamanhoAtual);
                
                byte[] dadosPacote = dataBuffer.array();
                clientSocket.send(new DatagramPacket(dadosPacote, dadosPacote.length, IPAddress, 9870));
                
                // Pequeno atraso para evitar perda de pacotes no UDP
                Thread.sleep(2);
            }
            
            // --- PACOTE END ---
            // Primeiro byte 'E' indicando o fim
            byte[] endBytes = new byte[] { (byte) 'E' };
            clientSocket.send(new DatagramPacket(endBytes, endBytes.length, IPAddress, 9870));
            
            System.out.println("Envio finalizado com sucesso!");
            clientSocket.close();
            
        } catch (Exception e) {
            System.out.println("Erro no Cliente: " + e.getMessage());
        }
    }
}