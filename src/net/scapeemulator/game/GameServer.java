package net.scapeemulator.game;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.script.ScriptException;

import net.scapeemulator.cache.Cache;
import net.scapeemulator.cache.ChecksumTable;
import net.scapeemulator.cache.FileStore;
import net.scapeemulator.game.cache.MapLoader;
import net.scapeemulator.game.content.Content;
import net.scapeemulator.game.io.DummyPlayerSerializer;
import net.scapeemulator.game.io.JdbcSerializer;
import net.scapeemulator.game.io.Serializer;
import net.scapeemulator.game.model.Ladders;
import net.scapeemulator.game.model.World;
import net.scapeemulator.game.model.definition.ItemDefinitions;
import net.scapeemulator.game.model.definition.NPCDefinitions;
import net.scapeemulator.game.model.definition.ObjectDefinitions;
import net.scapeemulator.game.model.definition.VarbitDefinitions;
import net.scapeemulator.game.model.definition.WidgetDefinitions;
import net.scapeemulator.game.model.object.GroundObjectPopulator;
import net.scapeemulator.game.model.pathfinding.MapDataListener;
import net.scapeemulator.game.model.player.EquipmentDefinition;
import net.scapeemulator.game.model.player.bank.BankerAction;
import net.scapeemulator.game.model.player.consumable.Consumables;
import net.scapeemulator.game.model.player.skills.construction.Construction;
import net.scapeemulator.game.model.player.skills.cooking.Cooking;
import net.scapeemulator.game.model.player.skills.farming.Farming;
import net.scapeemulator.game.model.player.skills.firemaking.Firemaking;
import net.scapeemulator.game.model.player.skills.fishing.Fishing;
import net.scapeemulator.game.model.player.skills.herblore.Herblore;
import net.scapeemulator.game.model.player.skills.magic.Magic;
import net.scapeemulator.game.model.player.skills.mining.Mining;
import net.scapeemulator.game.model.player.skills.prayer.PrayerSkill;
import net.scapeemulator.game.model.player.skills.runecrafting.Runecrafting;
import net.scapeemulator.game.model.player.skills.woodcutting.Woodcutting;
import net.scapeemulator.game.msg.CodecRepository;
import net.scapeemulator.game.msg.MessageDispatcher;
import net.scapeemulator.game.net.HttpChannelInitializer;
import net.scapeemulator.game.net.RsChannelInitializer;
import net.scapeemulator.game.net.login.LoginService;
import net.scapeemulator.game.net.update.UpdateService;
import net.scapeemulator.game.plugin.PluginLoader;
import net.scapeemulator.game.plugin.ScriptContext;
import net.scapeemulator.game.util.LandscapeKeyTable;
import net.scapeemulator.util.NetworkConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameServer {

    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);
    private static GameServer server;

    private static final boolean HTTP_SERVER_ENABLED = false;
    private static final boolean LOAD_MAPS_ON_STARTUP = false;

    public static void main(String[] args) {
        try {
            InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

            GameServer instance = getInstance();
            instance.load();

            if (HTTP_SERVER_ENABLED) {
                try {
                    instance.httpBind(new InetSocketAddress(NetworkConstants.HTTP_PORT));
                } catch (Throwable t) {
                    logger.warn("Failed to bind to HTTP port.", t);
                }
                instance.httpBind(new InetSocketAddress(NetworkConstants.HTTP_ALT_PORT));
            }

            try {
                instance.serviceBind(new InetSocketAddress(NetworkConstants.SSL_PORT));
            } catch (Throwable t) {
                logger.warn("Failed to bind to SSL port.", t);
            }
            instance.serviceBind(new InetSocketAddress(NetworkConstants.GAME_PORT));

            instance.start();
        } catch (Throwable t) {
            logger.error("Failed to start server.", t);
        }
    }

    private final World world = World.getWorld();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final EventLoopGroup loopGroup = new NioEventLoopGroup();
    private final ServerBootstrap serviceBootstrap = new ServerBootstrap();
    private final ServerBootstrap httpBootstrap = new ServerBootstrap();
    private final UpdateService updateService = new UpdateService();
    private final PluginLoader pluginLoader = new PluginLoader();
    private final ScriptContext scriptContext = new ScriptContext();
    private MapLoader mapLoader;
    private int ticks = 0;
    private final MessageDispatcher messageDispatcher = new MessageDispatcher();
    private LoginService loginService;
    private Serializer serializer;
    private Cache cache;
    private ChecksumTable checksumTable;
    private LandscapeKeyTable landscapeKeyTable;
    private CodecRepository codecRepository;

    private final int version = 530;

    private GameServer() {
    }

    private Serializer createPlayerSerializer() throws IOException, SQLException {
        Properties properties = new Properties();
        try (InputStream is = new FileInputStream("data/game/config/serializer.conf")) {
            properties.load(is);
        }

        String type = (String) properties.get("type");
        switch (type) {
            case "dummy":
                return new DummyPlayerSerializer();

            case "jdbc":
                String url = (String) properties.get("url");
                String username = (String) properties.get("username");
                String password = (String) properties.get("password");
                return new JdbcSerializer(url, username, password);

            default:
                throw new IOException("unknown serializer type");
        }
    }

    public void httpBind(SocketAddress address) throws InterruptedException {
        logger.info("Binding to HTTP address: " + address + "...");
        httpBootstrap.localAddress(address).bind().sync();
    }

    public void serviceBind(SocketAddress address) throws InterruptedException {
        logger.info("Binding to service address: " + address + "...");
        serviceBootstrap.localAddress(address).bind().sync();
    }

    public void load() throws IOException, ScriptException, SQLException {
        logger.info("Starting game server...");

        /* load landscape keys */

        landscapeKeyTable = LandscapeKeyTable.open("data/game/landscape-keys");

        /* load game cache */

        cache = new Cache(FileStore.open("data/game/cache"));
        checksumTable = cache.createChecksumTable();
        ItemDefinitions.init(cache);
        ObjectDefinitions.init(cache);
        WidgetDefinitions.init(cache);
        VarbitDefinitions.init(cache);
        NPCDefinitions.init(cache);
        EquipmentDefinition.init();

        /* load player serializer from config file */
        serializer = createPlayerSerializer();
        logger.info("Using serializer: " + serializer + ".");
        loginService = new LoginService(serializer);
        serializer.loadNPCDrops();
        serializer.loadNPCDefinitions();
        serializer.loadNPCSpawns();
        serializer.loadShops();

        /* load all the maps into memory */
        mapLoader = new MapLoader(cache, landscapeKeyTable);
        mapLoader.addListener(new GroundObjectPopulator(world.getGroundObjects()));
        mapLoader.addListener(new MapDataListener(world.getTraversalMap()));
        mapLoader.load(LOAD_MAPS_ON_STARTUP);

        /* load message codecs and dispatcher */
        logger.info("Populating codecs...");
        codecRepository = new CodecRepository(landscapeKeyTable);

        /* load the server pluginLoader */

        logger.info("Loading plugins...");
        pluginLoader.setContext(scriptContext);
        pluginLoader.load("./data/game/plugins/");

        /* decorate each of the dispatchers */
        messageDispatcher.decorateDispatchers(scriptContext);

        Ladders.init();

        /* bind the non-script skill handlers to the dispatchers */
        Fishing.initialize();
        Magic.initialize();
        Cooking.initialize();
        Mining.initialize();
        Firemaking.initialize();
        PrayerSkill.initialize();
        Herblore.initialize();
        Runecrafting.initialize();
        Woodcutting.initialize();
        Construction.initialize();
        Farming.initialize();

        /* bind other content */
        Consumables.initialize();
        messageDispatcher.getNpcDispatcher().bind(new BankerAction());
        Content.init();

        /* start netty */
        httpBootstrap.group(loopGroup);
        httpBootstrap.channel(NioServerSocketChannel.class);
        httpBootstrap.childHandler(new HttpChannelInitializer());
        serviceBootstrap.group(loopGroup);
        serviceBootstrap.channel(NioServerSocketChannel.class);
        serviceBootstrap.childHandler(new RsChannelInitializer(this));

    }

    public void start() {
        logger.info("Ready for connections.");

        /* start login and update services */
        executor.submit(loginService);
        executor.submit(updateService);

        /* main game tick loop */
        for (;;) {
            long start = System.currentTimeMillis();
            tick();
            long elapsed = (System.currentTimeMillis() - start);
            long waitFor = 600 - elapsed;
            if (waitFor > 0) {
                try {
                    Thread.sleep(waitFor);
                } catch (InterruptedException e) {
                    /* ignore */
                }
            }
        }
    }

    private void tick() {
        /* As the MobList class is not thread-safe, players must be registered within the game logic processing code. */
        ticks++;
        loginService.registerNewPlayers(world);

        world.tick();
    }

    public World getWorld() {
        return world;
    }

    public int getTickTimestamp() {
        return ticks;
    }

    public LoginService getLoginService() {
        return loginService;
    }

    public UpdateService getUpdateService() {
        return updateService;
    }

    public int getVersion() {
        return version;
    }

    public Cache getCache() {
        return cache;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public MapLoader getMapLoader() {
        return mapLoader;
    }

    public ChecksumTable getChecksumTable() {
        return checksumTable;
    }

    public CodecRepository getCodecRepository() {
        return codecRepository;
    }

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public LandscapeKeyTable getLandscapeKeyTable() {
        return landscapeKeyTable;
    }

    public static GameServer getInstance() {
        if (server == null) {
            server = new GameServer();
        }
        return server;
    }
}
