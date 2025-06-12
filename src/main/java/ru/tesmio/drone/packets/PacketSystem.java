package ru.tesmio.drone.packets;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import ru.tesmio.drone.Core;
import ru.tesmio.drone.packets.client.*;
import ru.tesmio.drone.packets.server.*;


//TODO: Реализовать методы оптимизации пакетов: квантование и предсказание как предлагает deepseek
// (с линейной интерполяцией) и отправкой не каждый тик, а каждые три-пять тиков, а на клиенте проводить
// интерполяцию между этими значениями. Доработать в стиле UDP - с возможностью потери пакетов
// добавить сжатие данных. Реализовать приоритезацию и буферизацию пакетов
public class PacketSystem {
    private static int id = 0;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Core.MODID, "main"),
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
        CHANNEL.registerMessage(id++, DroneFlightModePacket.class,
                DroneFlightModePacket::encode,
                DroneFlightModePacket::decode,
                DroneFlightModePacket::handle);
        CHANNEL.registerMessage(id++, DroneFlightModeServerPacket.class,
                DroneFlightModeServerPacket::encode,
                DroneFlightModeServerPacket::decode,
                DroneFlightModeServerPacket::handle);
        CHANNEL.registerMessage(id++, DistanceControlPacket.class,
                DistanceControlPacket::encode,
                DistanceControlPacket::decode,
                DistanceControlPacket::handle);
        CHANNEL.registerMessage(id++, DroneStabModePacket.class,
                DroneStabModePacket::encode,
                DroneStabModePacket::decode,
                DroneStabModePacket::handle);
        CHANNEL.registerMessage(id++, DroneStabModeServerPacket.class,
                DroneStabModeServerPacket::encode,
                DroneStabModeServerPacket::decode,
                DroneStabModeServerPacket::handle);
        CHANNEL.registerMessage(id++, DroneZoomModePacket.class,
                DroneZoomModePacket::encode,
                DroneZoomModePacket::decode,
                DroneZoomModePacket::handle);
        CHANNEL.registerMessage(id++, DroneZoomModeServerPacket.class,
                DroneZoomModeServerPacket::encode,
                DroneZoomModeServerPacket::decode,
                DroneZoomModeServerPacket::handle);
        CHANNEL.registerMessage(id++, DroneVisionModePacket.class,
                DroneVisionModePacket::encode,
                DroneVisionModePacket::decode,
                DroneVisionModePacket::handle);
        CHANNEL.registerMessage(id++, DroneVisionModeServerPacket.class,
                DroneVisionModeServerPacket::encode,
                DroneVisionModeServerPacket::decode,
                DroneVisionModeServerPacket::handle);
        CHANNEL.registerMessage(id++, DroneReconnectPacket.class,
                DroneReconnectPacket::encode,
                DroneReconnectPacket::decode,
                DroneReconnectPacket::handle);
        CHANNEL.registerMessage(id++, DroneModesSyncPacket.class,
                DroneModesSyncPacket::encode,
                DroneModesSyncPacket::decode,
                DroneModesSyncPacket::handle);
    }
}
