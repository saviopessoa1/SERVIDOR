import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ServidorTema07UDP {
    public static void main(String[] args) {
        try {
            DatagramSocket serverSocket = new DatagramSocket(9870);
            System.out.println("SERVIDOR UDP EM AÇÃO aguardando printscreens!!!");
            
            // Cria a pasta para guardar as imagens conforme exigência do Tema 07
            File pasta = new File("imagens_recebidas");
            if (!pasta.exists()) {
                pasta.mkdir();
            }
            
            // Loop infinito para continuar recebendo novas imagens
            while(true) {
                ByteArrayOutputStream bufferImagem = new ByteArrayOutputStream();
                boolean recebendo = true;
                
                System.out.println("Aguardando nova imagem do cliente...");
                
                // Loop para receber os pedaços (fragmentos) de uma mesma imagem
                while (recebendo) {
                    byte[] receiveData = new byte[60000]; // Buffer maior que o original de 1024
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    
                    int tamanhoRecebido = receivePacket.getLength();
                    String textoRecebido = new String(receivePacket.getData(), 0, tamanhoRecebido);
                    
                    // Verifica se o cliente enviou o pacote "fim", igual no exemplo original
                    if (textoRecebido.equals("fim")) {
                        recebendo = false;
                    } else {
                        // Vai juntando os pedaços da imagem na memória
                        bufferImagem.write(receivePacket.getData(), 0, tamanhoRecebido);
                    }
                }
                
                byte[] imagemCompleta = bufferImagem.toByteArray();
                if (imagemCompleta.length > 0) {
                    // Salva o arquivo na pasta
                    String nomeArquivo = "imagens_recebidas/print_" + System.currentTimeMillis() + ".jpg";
                    FileOutputStream fos = new FileOutputStream(nomeArquivo);
                    fos.write(imagemCompleta);
                    fos.close();
                    System.out.println("Imagem remontada e salva com sucesso: " + nomeArquivo);
                }
            }
        } catch (Exception e) {
            System.out.println("Erro no Servidor: " + e.getMessage());
        }
    }
}