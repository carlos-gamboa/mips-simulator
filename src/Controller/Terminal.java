package Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Terminal {

    BufferedReader br;

    public Terminal() {
        br = new BufferedReader(new InputStreamReader(System.in));
    }

//    public String getFiles() throws IOException {
//
//        //System.out.println("Digite el nombre de los hilillos separados por un espacio");
//        //String s = br.readLine();
//       return s;
//    }

    public int getQuantum() throws IOException {
        System.out.println("Digite el quantum para los hilillos");
        return Integer.parseInt(br.readLine());
    }

    public boolean getSimulationMode() throws IOException {
        System.out.println("Digite \"Si\" si desea ver la simulación en modo lento, de lo contrario la verá en modo rápido.");
        if (br.readLine().equals("Si")){
            return true;
        }
        else{
            return false;
        }
    }

}