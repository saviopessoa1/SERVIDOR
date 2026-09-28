import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;

public class ClienteTema07UDP {
    public static void main(String[] args) {
        System.out.println("CLIENTE AUTOMÁTICO INICIADO!");
        System.out.println("Basta apertar a tecla 'Print Screen' no teclado.");
        System.out.println("O programa vai detectar e enviar automaticamente...");

        try {
            DatagramSocket clientSocket = new DatagramSocket();
            // Lembre-se de colocar o IP correto aqui (localhost para testar na mesma máquina)
            InetAddress IPAddress = InetAddress.getByName("10.154.213.79");
            
            // Acessa a Área de Transferência (Clipboard) do sistema
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            
            // Loop infinito: o programa fica sempre à escuta de um novo print
            while (true) {
                try {
                    // Verifica se existe uma IMAGEM na área de transferência
                    if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                        System.out.println("\nNovo Print detetado! Iniciando envio para o Servidor...");
                        
                        // 1. Pega a imagem do clipboard
                        Image imagemClipboard = (Image) clipboard.getData(DataFlavor.imageFlavor);
                        
                        // 2. Converte para BufferedImage (formato necessário para converter em Bytes)
                        BufferedImage printScreen = new BufferedImage(
                                imagemClipboard.getWidth(null),
                                imagemClipboard.getHeight(null),
                                BufferedImage.TYPE_INT_RGB);
                        Graphics2D bGr = printScreen.createGraphics();
                        bGr.drawImage(imagemClipboard, 0, 0, null);
                        bGr.dispose();

                        // 3. Converte a imagem capturada para array de bytes (JPG)
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(printScreen, "jpg", baos);
                        byte[] imagemBytes = baos.toByteArray();
                        
                        // 4. Fatiamento da imagem (mantido o seu código original que está excelente)
                        int tamanhoPacote = 60000;
                        int totalPacotes = (int) Math.ceil((double) imagemBytes.length / tamanhoPacote);
                        
                        for (int i = 0; i < totalPacotes; i++) {
                            int inicio = i * tamanhoPacote;
                            int fim = Math.min(imagemBytes.length, inicio + tamanhoPacote);
                            int tamanhoAtual = fim - inicio;
                            
                            byte[] dadosPacote = new byte[tamanhoAtual];
                            System.arraycopy(imagemBytes, inicio, dadosPacote, 0, tamanhoAtual);
                            
                            DatagramPacket sendPacket = new DatagramPacket(dadosPacote, dadosPacote.length, IPAddress, 9870);
                            clientSocket.send(sendPacket);
                            
                            // Atraso crucial para não engasgar a rede
                            Thread.sleep(5);
                        }
                        
                        // 5. Envia pacote finalizador
                        byte[] fimBytes = "fim".getBytes();
                        DatagramPacket fimPacket = new DatagramPacket(fimBytes, fimBytes.length, IPAddress, 9870);
                        clientSocket.send(fimPacket);
                        
                        System.out.println("Envio automático finalizado com sucesso!");
                        
                        // 6. LIMPA O CLIPBOARD: Isso evita que o programa envie a mesma imagem repetidamente
                        StringSelection selection = new StringSelection("");
                        clipboard.setContents(selection, selection);
                    }
                } catch (Exception ex) {
                    // Ignora erros de leitura temporários do clipboard para o programa não crashar
                }
                
                // Aguarda 1 segundo antes de verificar o teclado novamente (não consome CPU)
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.out.println("Erro Crítico no Cliente: " + e.getMessage());
        }
    }
}