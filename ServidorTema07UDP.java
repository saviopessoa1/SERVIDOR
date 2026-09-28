import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

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
            
            while(true) {
                System.out.println("Aguardando nova imagem do cliente...");
                boolean recebendo = true;
                byte[] imagemRemontada = null;
                
                // Loop para receber os pacotes
                while (recebendo) {
                    byte[] receiveData = new byte[1500]; // Buffer seguro para cabeçalho + 1024 bytes
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    
                    ByteBuffer buffer = ByteBuffer.wrap(receivePacket.getData(), 0, receivePacket.getLength());
                    
                    // Lê o PRIMEIRO BYTE para saber se é INIT, DATA ou END
                    byte tipo = buffer.get(); 
                    
                    if (tipo == 'I') {
                        // Trata o pacote INIT: inicializa o buffer na memória com o tamanho total
                        int tamanhoTotal = buffer.getInt();
                        imagemRemontada = new byte[tamanhoTotal];
                        
                    } else if (tipo == 'D' && imagemRemontada != null) {
                        // Trata o pacote DATA: pega o número de sequência e remonta na ordem exata
                        int sequencia = buffer.getInt();
                        int tamanhoPayload = receivePacket.getLength() - 5; // 1 byte de tipo + 4 bytes de int
                        
                        // Calcula a posição de memória exata para colocar esse fragmento
                        int posicao = sequencia * 1024; 
                        
                        System.arraycopy(receivePacket.getData(), 5, imagemRemontada, posicao, tamanhoPayload);
                        
                    } else if (tipo == 'E') {
                        // Trata o pacote END: encerra o loop de recepção
                        recebendo = false;
                    }
                }
                
                if (imagemRemontada != null && imagemRemontada.length > 0) {
                    // Uso do SimpleDateFormat para gerar o nome do arquivo com a data e hora
                    SimpleDateFormat formatador = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
                    String dataHora = formatador.format(new Date());
                    
                    String nomeArquivo = "imagens_recebidas/print_" + dataHora + ".jpg";
                    FileOutputStream fos = new FileOutputStream(nomeArquivo);
                    fos.write(imagemRemontada);
                    fos.close();
                    System.out.println("Imagem remontada e salva com sucesso: " + nomeArquivo);
                }
            }
        } catch (Exception e) {
            System.out.println("Erro no Servidor: " + e.getMessage());
        }
    }
}