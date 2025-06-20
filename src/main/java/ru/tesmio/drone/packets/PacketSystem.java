package ru.tesmio.drone.packets;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import ru.tesmio.drone.Dronecraft;
import ru.tesmio.drone.packets.client.*;
import ru.tesmio.drone.packets.server.*;

import java.util.Optional;


//TODO: Реализовать методы оптимизации пакетов: квантование и предсказание как предлагает deepseek
// (с линейной интерполяцией) и отправкой не каждый тик, а каждые три-пять тиков, а на клиенте проводить
// интерполяцию между этими значениями. Доработать в стиле UDP - с возможностью потери пакетов
// добавить сжатие данных. Реализовать приоритезацию и буферизацию пакетов
public class PacketSystem {
    private static int id = 0;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Dronecraft.MODID, "main"),
            () -> "1.0",
            s -> true,
            s -> true
    );

    public static void initPackets() {
     CHANNEL.registerMessage(id++, DroneMovePacket.class,
             DroneMovePacket::encode,
    DroneMovePacket::decode,
    DroneMovePacket::handle);
        CHANNEL.registerMessage(id++, DroneViewPacket.class,
                DroneViewPacket::encode,
                DroneViewPacket::decode,
                DroneViewPacket::handle);
        CHANNEL.registerMessage(id++,DroneDeathPacket .class,
    DroneDeathPacket::encode,
    DroneDeathPacket::decode,
    DroneDeathPacket::handle);
        CHANNEL.registerMessage(id++,ActionBarMessagePacket .class,
    ActionBarMessagePacket::encode,
    ActionBarMessagePacket::decode,
    ActionBarMessagePacket::handle);
        CHANNEL.registerMessage(id++,DroneControllerPacket .class,
    DroneControllerPacket::encode,
    DroneControllerPacket::decode,
    DroneControllerPacket::handle);

        CHANNEL.registerMessage(id++, DistanceControlPacket.class,
                DistanceControlPacket::encode,
                DistanceControlPacket::decode,
                DistanceControlPacket::handle);
        CHANNEL.registerMessage(id++, DroneInventorySyncPacket.class,
                DroneInventorySyncPacket::encode,
                DroneInventorySyncPacket::decode,
                DroneInventorySyncPacket::handle);

        CHANNEL.registerMessage(id++, DroneReconnectPacket.class,
                DroneReconnectPacket::encode,
                DroneReconnectPacket::decode,
                DroneReconnectPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, DroneModesPacket.class,
                DroneModesPacket::encode,
                DroneModesPacket::decode,
                DroneModesPacket::handle);
        CHANNEL.registerMessage(id++, DroneModesC2SP.class,
                DroneModesC2SP::encode,
                DroneModesC2SP::decode,
                DroneModesC2SP::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
