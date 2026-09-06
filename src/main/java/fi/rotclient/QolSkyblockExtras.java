package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Persisted SkyBlock extras. Nested on {@link QolUtilityConfig}
 * so Gson round-trips them without exploding every existing switch.
 */
final class QolSkyblockExtras {
    static final String STYLE_SIMPLE = "Simple";
    static final String STYLE_HOTBAR = "Hotbar";
    static final String STYLE_CUSTOM = "Custom";
    static final String VIGNETTE_NONE = "None";
    static final String VIGNETTE_AMBIENT = "Ambient";
    static final String VIGNETTE_DANGER = "Danger";
    static final String VIGNETTE_BOTH = "Both";

    boolean infoTooltipsEnabled;
    boolean infoDungeonQuality = true;
    boolean infoCreatedDate = true;
    boolean infoHexColor = true;
    boolean infoMuseum = true;
    boolean infoItemId = true;
    boolean infoStarCount = true;
    boolean infoPetCandy = true;
    boolean infoRevertMasterStars = true;

    boolean priceTooltipsEnabled;
    boolean priceLowestBin = true;
    boolean priceBazaar = true;
    boolean priceNpc = true;
    boolean priceMotes = true;
    boolean pricePaid;
    int priceBurgerCount;
    Map<String, Long> pricePaidByUuid = new LinkedHashMap<>();

    boolean viewmodelEnabled;
    boolean viewmodelNoHaste;
    boolean viewmodelNoEquip;
    boolean viewmodelNoBowSwing;
    boolean viewmodelApplyToHand;
    int viewmodelSwingSpeed;
    double viewmodelOffsetX;
    double viewmodelOffsetY;
    double viewmodelOffsetZ;
    double viewmodelScaleX = 1.0D;
    double viewmodelScaleY = 1.0D;
    double viewmodelScaleZ = 1.0D;
    double viewmodelRotX;
    double viewmodelRotY;
    double viewmodelRotZ;
    double viewmodelSwingX = 1.0D;
    double viewmodelSwingY = 1.0D;
    double viewmodelSwingZ = 1.0D;

    boolean animationFixEnabled;
    boolean animationDyes;
    boolean animationSkins;
    boolean disconnectFixEnabled;
    boolean doubleUseFixEnabled;
    boolean eyeHeightFixEnabled;
    boolean instantSneakEnabled;
    boolean itemCountFixEnabled;

    boolean itemScaleEnabled;
    double itemScale = 1.0D;

    boolean activePetHighlightEnabled;
    int activePetHighlightColor = 0xFF22C55E;
    boolean anvilHelperEnabled;
    int anvilHelperColor = 0xFF22C55E;
    boolean calendarDateEnabled;
    boolean calendarMinister = true;

    boolean experimentSolverEnabled;
    boolean experimentChronomatron = true;
    boolean experimentUltrasequencer = true;
    boolean experimentSuperpairs = true;
    boolean experimentBlockWrongClicks = true;
    boolean experimentHideTooltip = true;
    boolean experimentHideWrongChrono;
    boolean experimentHideWrongUltra;
    boolean experimentPrivateIslandOnly = true;
    int experimentFirstColor = 0x8022C55E;
    int experimentSecondColor = 0x80FACC15;
    int experimentMatchedColor = 0x8022C55E;
    int experimentMatchColor = 0x80FACC15;
    int experimentPowerupColor = 0x80FF00FF;

    boolean autoExperimentsEnabled;
    int autoExperimentsClickDelay = 200;
    int autoExperimentsDelayVariety = 50;
    boolean autoExperimentsAutoClose = true;
    int autoExperimentsSerumCount;
    boolean autoExperimentsGetMaxXp;

    boolean cheaterWardrobeEnabled;
    boolean cheaterWardrobeMoveEquip = true;
    boolean cheaterWardrobeStationaryOnly = true;
    boolean cheaterWardrobeResetOpen = true;
    int cheaterWardrobeClickDelay = 1;
    int cheaterWardrobeCloseDelay = 1;
    int cheaterWardrobeDelayVariance = 1;
    String cheaterWardrobeSlot1 = "";
    String cheaterWardrobeSlot2 = "";
    String cheaterWardrobeSlot3 = "";
    String cheaterWardrobeSlot4 = "";
    String cheaterWardrobeSlot5 = "";
    String cheaterWardrobeSlot6 = "";
    String cheaterWardrobeSlot7 = "";
    String cheaterWardrobeSlot8 = "";
    String cheaterWardrobeSlot9 = "";

    boolean escrowFixEnabled;

    boolean autoHarpEnabled;

    boolean autoGfsEnabled;
    boolean autoGfsInSkyblock = true;
    boolean autoGfsInKuudra = true;
    boolean autoGfsInDungeon = true;
    boolean autoGfsRefillOnDungeonStart = true;
    boolean autoGfsRefillOnTimer;
    int autoGfsTimerIncrements = AutoGfsPolicy.DEFAULT_TIMER_SECONDS;
    boolean autoGfsRefillPearl = true;
    boolean autoGfsRefillJerry = true;
    boolean autoGfsRefillTnt = true;
    boolean autoGfsRefillLeap;
    boolean autoGfsRefillTwilight;
    boolean autoGfsAutoGetDraft = true;
    String autoGfsKeybind = "";

    boolean autoSellEnabled;
    int autoSellDelay = AutoSellPolicy.DEFAULT_DELAY;
    int autoSellRandomization = AutoSellPolicy.DEFAULT_RANDOMIZATION;
    String autoSellClickType = AutoSellPolicy.CLICK_SHIFT;
    java.util.List<String> autoSellItems = new java.util.ArrayList<>();
    String autoSellKeybind = "";

    boolean ghostsEnabled;
    boolean ghostsShowGhosts;
    boolean ghostsShowPowered;
    String ghostsHighlightStyle = GhostsPolicy.DEFAULT_HIGHLIGHT;
    int ghostsFillColor = GhostsPolicy.DEFAULT_FILL;
    int ghostsOutlineColor = GhostsPolicy.DEFAULT_OUTLINE;
    String ghostsKeybind = "";

    boolean autoDojoEnabled;
    boolean autoDojoControl = true;
    boolean autoDojoMastery = true;
    boolean autoDojoDiscipline = true;
    boolean autoDojoDisciplineAttack = true;
    int autoDojoControlPredict = 5;
    int autoDojoMasteryDelay = 600;

    boolean fishingCreaturesEnabled;
    boolean fishingCreaturesHud = true;
    boolean fishingCreaturesCapNotify = true;
    boolean fishingCreaturesTimerNotify = true;
    int fishingCreaturesTimerLength = FishingCreaturesPolicy.DEFAULT_TIMER_SECONDS;
    String fishingCreaturesMinRarity = FishingCreaturesPolicy.DEFAULT_MIN_RARITY;
    boolean fishingCreaturesRareAnnounce = true;
    boolean fishingCreaturesRareSound = true;
    boolean fishingCreaturesRareParty;
    boolean fishingCreaturesRareEsp = true;
    int fishingCreaturesEspColor = 0xFF55FFFF;
    boolean fishingCreaturesShortenChat;
    boolean fishingCreaturesHideCommon;
    boolean fishingCreaturesAutoAttack;
    int fishingCreaturesAutoDelay = FishingCreaturesPolicy.DEFAULT_AUTO_DELAY;
    boolean fishingCreaturesThunderSparks = true;

    boolean fishingHotspotsEnabled;
    boolean fishingHotspotsCircle = true;
    boolean fishingHotspotsHideParticles;
    boolean fishingHotspotsRadar;
    boolean fishingHotspotsTracer = true;
    boolean fishingHotspotsDespawn = true;
    int fishingHotspotsColor = 0xFFFFAA00;

    boolean fishingTrophyEnabled;
    boolean fishingTrophyTitles = true;
    boolean fishingTrophyFilterChat;
    String fishingTrophyMinRarity = FishingTrophyPolicy.DEFAULT_MIN_RARITY;
    boolean fishingTrophyGoldenTimer = true;
    boolean fishingTrophyGeyser = true;
    boolean fishingTrophySponge = true;
    boolean fishingTrophyFillet = true;

    boolean fishingVisualsEnabled;
    boolean fishingVisualsHideOtherBobbers;
    boolean fishingVisualsChumHider;
    boolean fishingVisualsMuteBanshee = true;
    boolean fishingVisualsMuteDrake = true;

    boolean fishingToolsEnabled;
    boolean fishingToolsBaitHud = true;
    boolean fishingToolsNoBaitWarn = true;
    boolean fishingToolsBaitChange = true;
    boolean fishingToolsThunderNotify = true;
    boolean fishingToolsTotemHud = true;

    boolean miningScathaEnabled;
    boolean miningScathaTitles = true;
    boolean miningScathaSounds = true;
    boolean miningScathaCooldown = true;
    boolean miningScathaPetDrop = true;
    boolean miningScathaPetRarity = true;
    boolean miningScathaHud = true;
    boolean miningScathaParty;

    boolean miningEventsEnabled;
    boolean miningEventsHud = true;
    boolean miningEventsTitles = true;
    boolean miningEventsGoblinEsp;

    boolean miningGlaciteEnabled;
    boolean miningGlacitePityHud = true;
    boolean miningGlaciteCorpseHud = true;
    boolean miningGlaciteColdOverlay;
    boolean miningGlacitePartyShare;
    boolean miningGlaciteShaftParty;
    boolean miningGlacitePityChat = true;
    boolean miningGlaciteEnterTitle = true;
    boolean miningGlaciteEnterChat = true;
    boolean miningGlaciteCorpseWaypoints = true;
    boolean miningGlaciteKeyAnnounce = true;
    boolean miningGlaciteEnterParty;

    boolean miningHelpersEnabled;
    boolean miningHelpersFetchur = true;
    boolean miningHelpersFossilMuncher = true;
    boolean miningHelpersDrillFuel = true;
    boolean miningHelpersAbilityHud = true;
    boolean miningHelpersCommissionGui = true;
    boolean miningHelpersCommissionMobs;
    boolean miningHelpersNotifyPortal = true;
    boolean miningHelpersNotifyScrap = true;
    boolean miningHelpersNotifyGoblin = true;
    boolean miningHelpersMetalDistance = true;
    boolean miningHelpersDetectorSolver;
    boolean miningHelpersDetectorDing = true;
    boolean miningHelpersDetectorTitle = true;
    boolean miningHelpersRedCarpets = true;
    boolean miningHelpersFossilExcavator;
    boolean miningHelpersWishingCompass;
    boolean miningHelpersCallKing;
    boolean miningHelpersBreakReset = true;
    boolean miningHelpersGemstoneDesync = true;

    boolean miningHotmEnabled;
    boolean miningHotmSkyMall = true;
    boolean miningHotmScreenHint = true;
    float miningHudX = 12.0F;
    float miningHudY = 220.0F;

    boolean dianaBurrowsEnabled;
    boolean dianaBurrowsGuess = true;
    boolean dianaBurrowsParticles = true;
    boolean dianaBurrowsWaypoints = true;
    boolean dianaBurrowsMuteSpade = true;
    boolean dianaBurrowsFixChat = true;
    int dianaBurrowsGuessColor = 0xFF55FF55;
    int dianaBurrowsStartColor = 0xFF55FFFF;
    int dianaBurrowsMobColor = 0xFFFF5555;
    int dianaBurrowsTreasureColor = 0xFFFFAA00;
    float dianaHudX = 12.0F;
    float dianaHudY = 292.0F;

    boolean dianaMobsEnabled;
    boolean dianaMobsRareEsp = true;
    boolean dianaMobsGriffinWarn = true;
    int dianaMobsEspColor = 0xFFFF55FF;

    boolean dianaProfitEnabled;
    boolean dianaProfitHud = true;

    boolean dianaShareEnabled;
    boolean dianaShareParty;
    boolean dianaShareAutoWarp;

    boolean foragingTreesEnabled;
    boolean foragingTreesProgressHud = true;
    boolean foragingTreesOnlyAxe;
    boolean foragingTreesHideBits = true;
    boolean foragingTreesGiftHud = true;
    boolean foragingTreesHideUnmineable = true;
    boolean foragingTreesFellTitle = true;

    boolean foragingAudioEnabled;
    boolean foragingAudioMutePhantom = true;
    boolean foragingAudioMuteTreeBreak = true;
    boolean foragingAudioMuteBreakGalatea = true;
    boolean foragingAudioMuteFusion = true;
    boolean foragingAudioMuteStereo;

    boolean foragingHelpersEnabled;
    boolean foragingHelpersSweepHud = true;
    boolean foragingHelpersTempleSolver = true;
    boolean foragingHelpersBeaconHints = true;
    boolean foragingHelpersHighlights = true;
    boolean foragingHelpersMoongladeBeacon = true;
    boolean foragingHelpersParkTutorial;
    boolean foragingHelpersHuntingEsp;
    boolean foragingHelpersFrogMask = true;
    boolean foragingHelpersLassoHud = true;
    boolean foragingHelpersCinderbat = true;
    boolean foragingHelpersHuntaxeLock;
    boolean foragingHelpersShardTracker = true;
    boolean foragingHelpersLassoAlert;
    int foragingHelpersSeaLumiesMin = 3;
    boolean foragingHelpersHotfHint = true;

    boolean foragingCheatsEnabled;
    boolean foragingCheatsAutoBeacon;
    boolean foragingCheatsAutoChop;
    boolean foragingCheatsAxeToss;
    int foragingCheatsMinCluster = 5;
    int foragingCheatsClickDelay = 3;
    float foragingHudX = 12.0F;
    float foragingHudY = 364.0F;

    boolean dungeonHudEnabled;
    boolean dungeonHudSecrets = true;
    boolean dungeonHudScore = true;
    boolean dungeonHudClass = true;
    boolean dungeonHudFloor = true;
    boolean dungeonHudCleared = true;
    boolean dungeonHudInvincibility = true;
    boolean dungeonHudMaskOverlay = true;
    int dungeonHudMaskOverlayColor = DungeonPolicy.MASK_OVERLAY_COLOR;
    boolean dungeonHudTerracotta = true;
    boolean dungeonHudBlessings = true;
    boolean dungeonHudF7Timers = true;
    boolean dungeonHudRagnarock = true;
    boolean dungeonHudMelody = true;
    boolean dungeonHudMelodyOther = true;
    boolean dungeonHudQuiz = true;
    boolean dungeonHudMap = true;
    String dungeonHudMapMode = DungeonMapPolicy.MAP_MODE_EXPLORED;
    boolean dungeonHudMapDoors = true;
    boolean dungeonHudMapPlayers = true;
    boolean dungeonHudMapExtra = true;
    boolean dungeonHudMapHideBoss;
    boolean dungeonHudCrypts = true;
    boolean dungeonHudDeaths = true;
    boolean dungeonHudScoreOverlay = true;
    boolean dungeonHudClassIcons = true;
    boolean dungeonHudHeadMarkers = true;
    boolean dungeonHudRoomNames = true;
    boolean dungeonHudRoomSecrets = true;
    boolean dungeonHudPlayerNames = true;
    boolean dungeonHudMapMimic = true;
    boolean dungeonHudMapPuzzles = true;
    int dungeonHudMapScale = DungeonMapPolicy.HUD_CELL;
    boolean dungeonHudPuzzleTimer = true;
    boolean dungeonHudWarpCooldown = true;
    boolean dungeonHudSecretSpawn = true;
    boolean dungeonHudExplosiveShot = true;
    boolean dungeonHudUnclaimedChests = true;
    boolean dungeonHudChestWarning = true;
    int dungeonHudChestWarningCount = DungeonBladePolicy.CHEST_WARNING_DEFAULT;
    boolean dungeonHudExtraStats = true;
    boolean dungeonHudLedge = true;
    boolean dungeonHudLedgeYellow = true;
    boolean dungeonHudLedgeAll;
    boolean dungeonHudRunTimers = true;
    boolean dungeonHudShowSplitPbs = true;
    String dungeonSplitPbs = "";
    boolean dungeonHudKuudraSplits;
    String kuudraSplitPbs = "";
    boolean dungeonHudCheaterMap;
    boolean dungeonHudCheaterNames = true;
    boolean dungeonHudCheaterDarken = true;
    double dungeonHudCheaterDarkenFactor = 0.6D;
    boolean dungeonEspEnabled;
    boolean dungeonEspStarred = true;
    boolean dungeonEspTeammates = true;
    boolean dungeonEspBats = true;
    boolean dungeonEspFels = true;
    boolean dungeonEspShadow = true;
    boolean dungeonEspKeys = true;
    boolean dungeonEspMimic = true;
    boolean dungeonEspTracers;
    boolean dungeonEspDepth = true;
    boolean dungeonEspGhostBlock;
    boolean dungeonEspGhostUayor;
    boolean dungeonEspGhostStonk = true;
    String dungeonEspGhostKeybind = "";
    boolean dungeonEspTriggerBot;
    boolean dungeonEspTriggerCrystal;
    boolean dungeonEspTriggerTake = true;
    boolean dungeonEspTriggerPlace = true;
    boolean dungeonEspTriggerSecret;
    int dungeonEspTriggerDelay = 200;
    boolean dungeonEspFill = true;
    int dungeonEspOpacity = 40;
    int dungeonEspStarredColor = 0xFFFFD700;
    int dungeonEspBatColor = 0xFF55FFFF;
    int dungeonEspFelColor = 0xFFAA00AA;
    int dungeonEspKeyColor = 0xFFAA0000;
    int dungeonEspBloodKeyColor = 0xFFFF5555;
    int dungeonEspShadowColor = 0xFFFF55FF;
    int dungeonEspTeammateColor = 0xFF55FF55;
    int dungeonEspMimicColor = 0xFFFF5555;
    boolean dungeonEspWither = true;
    boolean dungeonEspCrystals = true;
    boolean dungeonEspSecrets;
    boolean dungeonEspSimon = true;
    int dungeonEspWitherColor = 0xFF111111;
    int dungeonEspCrystalColor = 0xFF55FFFF;
    int dungeonEspSecretColor = 0xFF22D3EE;
    int dungeonEspChestColor = 0xFFFF55FF;
    int dungeonEspSimonColor = 0xFFFFFF00;
    boolean dungeonEspSecretWaypoints = true;
    boolean dungeonEspHideCollected = true;
    boolean dungeonEspSecretClicked = true;
    int dungeonEspSecretClickedColor = TempleDungeonPolicy.SECRET_CLICKED_COLOR;
    int dungeonEspSecretClickedLockedColor = TempleDungeonPolicy.SECRET_LOCKED_COLOR;
    int dungeonEspSecretClickedSeconds = TempleDungeonPolicy.SECRET_CLICKED_DEFAULT_SECONDS;
    boolean dungeonEspSecretClickedBoss;
    boolean dungeonEspItems = true;
    boolean dungeonEspIcedMobs = true;
    boolean dungeonEspHateDoors = true;
    boolean dungeonEspHateWither = true;
    boolean dungeonEspHateBlood = true;
    boolean dungeonEspHateEntrance;
    String dungeonEspHateWitherGlass = "Black";
    String dungeonEspHateBloodGlass = "Red";
    String dungeonEspHateEntranceGlass = "White";
    boolean dungeonEspLivid = true;
    boolean dungeonEspThorn = true;
    boolean dungeonEspSpiritBear = true;
    boolean dungeonEspDoors = true;
    boolean dungeonEspBloodBox = true;
    int dungeonEspLividColor = 0xFFFF55FF;
    int dungeonEspThornColor = 0xFFFF0000;
    int dungeonEspSpiritBearColor = 0xFFFFFFFF;
    int dungeonEspBloodBoxColor = 0xFFFF5555;
    int dungeonEspBloodLineColor = 0xFFFFAA00;
    boolean dungeonAnnounceEnabled;
    boolean dungeonAnnounceMimic = true;
    boolean dungeonAnnouncePrince = true;
    boolean dungeonAnnounceBat = true;
    boolean dungeonAnnounceBlood = true;
    boolean dungeonAnnounceScoreTitle = true;
    int dungeonAnnounceScoreThreshold = 270;
    boolean dungeonAnnounceF7 = true;
    boolean dungeonAnnounceRagnarock = true;
    boolean dungeonAnnounceRooms = true;
    boolean dungeonAnnounceMelody = true;
    boolean dungeonAnnounceMelodyParty;
    String dungeonAnnounceMelodyMessage = DungeonBladePolicy.DEFAULT_MELODY_PARTY;
    boolean dungeonAnnounceMelodyProgress;
    boolean dungeonAnnounceDeath;
    String dungeonAnnounceDeathMessage = DungeonBladePolicy.DEFAULT_DEATH_MESSAGE;
    boolean dungeonAnnouncePosition;
    boolean dungeonAnnounceSecretChime = true;
    boolean dungeonAnnounceDuplicateClass = true;
    boolean dungeonAnnouncePlayerCount = true;
    boolean dungeonAnnounceLocation = true;
    boolean dungeonAnnounceKeyDrop = true;
    boolean dungeonAnnounceKeyDropAll;
    boolean dungeonAnnounceAutoUlt;
    boolean dungeonLeapEnabled;
    boolean dungeonLeapHighlight = true;
    boolean dungeonLeapCustomGui = true;
    boolean dungeonLeapAnnounce;
    boolean dungeonLeapCounter = true;
    String dungeonLeapMessage = "ILY {name}";
    boolean dungeonTerminalsEnabled;
    boolean dungeonTerminalsOverlay = true;
    boolean dungeonTerminalsAuto;
    boolean dungeonTerminalsMelody = true;
    boolean dungeonTerminalsNumbers = true;
    boolean dungeonTerminalsNumbersShow;
    boolean dungeonTerminalsRubix = true;
    boolean dungeonTerminalsColors = true;
    boolean dungeonTerminalsPanes = true;
    boolean dungeonTerminalsStarts = true;
    boolean dungeonTerminalsAutoMelody = true;
    boolean dungeonTerminalsMelodySkip;
    boolean dungeonTerminalsMelodySkipFirstRow;
    String dungeonTerminalsMelodySkipMode = "Edges";
    boolean dungeonTerminalsSounds;
    boolean dungeonTerminalsCompleteSounds;
    boolean dungeonTerminalsStopTooltips = true;
    boolean dungeonTerminalsHideClicked;
    boolean dungeonTerminalsBlockWrongSlots;
    boolean dungeonTerminalsHumanOrder = true;
    boolean dungeonTerminalsMelodyKeys = true;
    boolean dungeonTerminalsProtect;
    int dungeonTerminalsProtectMs = 400;
    boolean dungeonTerminalsAutoNumbers = true;
    boolean dungeonTerminalsAutoColors = true;
    boolean dungeonTerminalsAutoRubix = true;
    boolean dungeonTerminalsAutoPanes = true;
    boolean dungeonTerminalsAutoStarts = true;
    boolean dungeonTerminalsQueue;
    boolean dungeonTerminalsClone = true;
    int dungeonTerminalsDelay = 4;
    int dungeonTerminalsColor = 0x8022C55E;
    boolean dungeonTerminalsHitboxes = true;
    int dungeonTerminalsHitboxColor = 0xFFFFA500;
    int dungeonTerminalsNumbers1Color = 0x8000FF00;
    int dungeonTerminalsNumbers2Color = 0x8000C800;
    int dungeonTerminalsNumbers3Color = 0x80009600;
    int dungeonTerminalsRubixPosColor = 0x800072FF;
    int dungeonTerminalsRubixNegColor = 0x80CD0000;
    int dungeonTerminalsMelodyColumnColor = 0x80FF00FF;
    int dungeonTerminalsMelodyIndicatorColor = 0x80FF7400;
    int dungeonTerminalsMelodyWrongColor = 0x80FF0000;
    boolean dungeonTermSimEnabled;
    String dungeonTermSimKeybind = "";
    int dungeonTermSimPing;
    boolean dungeonTermSimShowPbs = true;
    String dungeonTermSimPbs = "";
    boolean dungeonRequeueEnabled;
    int dungeonRequeueDelay = DungeonPolicy.DEFAULT_REQUEUE_DELAY_TICKS;
    boolean dungeonPuzzlesEnabled;
    boolean dungeonPuzzlesQuiz = true;
    boolean dungeonPuzzlesQuizBoxes = true;
    boolean dungeonPuzzlesQuizTimer = true;
    boolean dungeonPuzzlesWeirdos = true;
    boolean dungeonPuzzlesBlaze = true;
    boolean dungeonPuzzlesIce = true;
    boolean dungeonPuzzlesIcePath = true;
    boolean dungeonPuzzlesIceOptimize;
    boolean dungeonPuzzlesWater = true;
    boolean dungeonPuzzlesWaterOptimized;
    boolean dungeonPuzzlesBoulder = true;
    boolean dungeonPuzzlesTpMaze = true;
    boolean dungeonPuzzlesCreeperBeams = true;
    boolean dungeonPuzzlesTicTacToe = true;
    boolean dungeonF7Enabled;
    boolean dungeonF7Titles = true;
    boolean dungeonF7Timers = true;
    boolean dungeonF7TimerTicks;
    boolean dungeonF7TimerSymbol = true;
    boolean dungeonF7TimerPrefix = true;
    boolean dungeonF7Simon = true;
    boolean dungeonF7SimonAuto;
    boolean dungeonF7HideDiorite = true;
    boolean dungeonF7ArrowAlign = true;
    boolean dungeonF7I4 = true;
    boolean dungeonF7AutoI4;
    boolean dungeonF7AutoI4Rod;
    boolean dungeonF7AutoI4Mask;
    boolean dungeonF7AutoI4Leap;
    boolean dungeonF7AutoI4LeapMelody;
    String dungeonF7AutoI4LeapClass = "Tank";
    int dungeonF7AutoI4Rotation = 170;
    boolean dungeonF7Debuff = true;
    boolean dungeonF7DebuffAuto;
    boolean dungeonF7DebuffIce = true;
    boolean dungeonF7DebuffGravity = true;
    boolean dungeonF7Gate = true;
    boolean dungeonF7Relics = true;
    boolean dungeonF7RelicLook;
    int dungeonF7RelicLookTime = 150;
    boolean dungeonF7BreakerPreventSecrets = true;
    boolean dungeonF7BreakerCharges = true;
    boolean dungeonF7AutoSuperboom;
    boolean dungeonF7SuperboomSwapBack;
    int dungeonF7SuperboomDelay = 2;
    int dungeonF7GateColor = 0x80F97316;
    int dungeonF7I4Color = 0xFF22C55E;
    boolean dungeonF7TitleCrystal = true;
    boolean dungeonF7TitleWither = true;
    boolean dungeonF7TitleTerminal = true;
    boolean dungeonF7TitleGate = true;
    boolean dungeonF7HideOtherTitles = true;
    boolean dungeonF7HideTitlesAtSs = true;
    boolean dungeonF7HideTitlesAtPre4 = true;
    boolean dungeonF7HideAtSs = true;
    boolean dungeonF7HideAtSsPreTerms = true;
    boolean dungeonF7HideAfterLeap = true;
    boolean dungeonF7HideAfterLeapBoss = true;
    String dungeonF7TitleCrystalText = "Crystals {current}/{total}";
    String dungeonF7TitleWitherText = "{name} Enraged";
    String dungeonF7TitleTerminalText = "{name} {current}/{total}";
    String dungeonF7TitleGateText = "{name} {current}/{total}";
    boolean dungeonF7TimerMaxor;
    boolean dungeonF7TimerStorm;
    boolean dungeonF7TimerPad = true;
    boolean dungeonF7TimerLightning = true;
    boolean dungeonF7TimerGoldor = true;
    boolean dungeonF7TimerNecron = true;
    boolean dungeonF7Crystals = true;
    boolean dungeonF7MaxorStun = true;
    boolean dungeonF7StormCrush = true;
    boolean dungeonF7StormLb = true;
    boolean dungeonF7TermStart = true;
    boolean dungeonF7CrystalSpawn = true;
    boolean dungeonF7CrystalPlace = true;
    boolean dungeonF7CrystalAlert = true;
    boolean dungeonF7MelodyDisplay = true;
    boolean dungeonF7WitherEsp = true;
    int dungeonF7MaxorColor = 0xFF5804A4;
    int dungeonF7StormColor = 0xFF00D0FF;
    int dungeonF7GoldorColor = 0xFFE5E7EB;
    int dungeonF7NecronColor = 0xFFE11D48;
    boolean dungeonF7Dragons;
    boolean dungeonF7DragonSpray = true;
    boolean dungeonF7DragonArrows = true;
    boolean dungeonF7DragonHealth = true;
    boolean dungeonF7DragonBoxes = true;
    boolean dungeonF7DragonTracers;
    boolean dungeonF7DragonPriority;
    boolean dungeonF7DragonPaul;
    String dungeonF7DragonSoloClass = "Tank";
    boolean dungeonF7P3Display = true;
    boolean dungeonF7SharpShooter = true;
    boolean dungeonF7SharpAim;
    boolean dungeonF7SharpComplete = true;
    int dungeonF7SharpMarkedColor = DungeonGoldorPolicy.MARKED_COLOR;
    int dungeonF7SharpTargetColor = DungeonGoldorPolicy.TARGET_COLOR;
    int dungeonF7SharpAim1Color = DungeonGoldorPolicy.FIRST_AIM_COLOR;
    int dungeonF7SharpAim2Color = DungeonGoldorPolicy.SECOND_AIM_COLOR;
    int dungeonF7SharpAim3Color = DungeonGoldorPolicy.THIRD_AIM_COLOR;
    boolean dungeonF7TermTimes = true;
    boolean dungeonF7TermPbs = true;
    String dungeonF7TermPbTimes = "";
    boolean dungeonF7Predev = true;
    boolean dungeonF7PredevAll;
    long dungeonF7PredevPbMs;
    boolean dungeonF7SsComplete = true;
    boolean dungeonF7Pre4Complete = true;
    boolean dungeonF7DragonTimer = true;
    boolean dungeonF7GoldorFrenzy;
    boolean dungeonF7PurplePad;
    boolean dungeonLeapKeys = true;
    boolean dungeonF7SimonProgress;
    int dungeonF7SimonFirstColor = 0xFF22C55E;
    int dungeonF7SimonSecondColor = 0xFFFACC15;
    int dungeonF7SimonOtherColor = 0xFF38BDF8;
    boolean dungeonF7SimonBlockWrong;
    boolean dungeonF7SimonTrigger;
    boolean dungeonF7SimonSounds;
    boolean dungeonF7ArrowBlockWrong;
    boolean dungeonF7I4Predict = true;
    int dungeonF7I4PredictColor = 0xFF38BDF8;
    boolean dungeonF7RelicSpawn;
    int dungeonF7RelicSpawnTicks = DungeonF7Policy.DEFAULT_RELIC_SPAWN_TICKS;
    boolean dungeonF7RelicBeacon = true;
    boolean dungeonF7RelicPlace;
    boolean dungeonF7RelicHighlight = true;
    boolean dungeonF7RelicBlockWrong;
    boolean dungeonMenusEnabled;
    boolean dungeonMenusSalvage = true;
    boolean dungeonMenusPartyFinder = true;
    boolean dungeonMenusChestProfit;
    boolean dungeonMenusChestSpin = true;
    boolean dungeonMenusIncludeEssence = true;
    boolean dungeonMenusIncludeCost = true;
    boolean dungeonMenusCompactProfit;
    int dungeonMenusPartyCata;
    int dungeonMenusSalvage50Color = 0x8022C55E;
    int dungeonMenusSalvageLowColor = 0x80FACC15;
    int dungeonMenusProfitColor = 0x80F59E0B;
    boolean dungeonMenusCloseChest;
    String dungeonMenusCloseChestMode = "Auto";
    DungeonAthenSettings athen = new DungeonAthenSettings();
    CustomScoreboardSettings customScoreboard = new CustomScoreboardSettings();

    DungeonAthenSettings athen() {
        if (athen == null) {
            athen = new DungeonAthenSettings();
        }
        return athen;
    }

    CustomScoreboardSettings board() {
        if (customScoreboard == null) {
            customScoreboard = new CustomScoreboardSettings();
        }
        return customScoreboard;
    }
    boolean cameraClip;
    boolean cameraCustomDistance;
    double cameraDistance = TempleDungeonPolicy.DEFAULT_CAMERA_DISTANCE;

    boolean slayerDisplayEnabled;
    boolean slayerDisplayKillTime = true;
    boolean slayerDisplayDynamicSize = true;
    boolean slayerTimeMessagesEnabled;
    boolean slayerTimeMessagesTimeToKill = true;
    boolean slayerTimeMessagesPersonalBest;
    boolean slayerTimeMessagesQuestComplete = true;
    boolean slayerTimeMessagesCompact;
    java.util.Map<String, Long> slayerPersonalBests = new java.util.LinkedHashMap<>();
    boolean slayerProgressEnabled;
    boolean slayerProgressShowRemaining = true;
    boolean slayerProgressBossWarning = true;
    boolean slayerProgressWarningRepeat;
    int slayerProgressWarningPercent = 80;
    boolean slayerStatsEnabled;
    boolean slayerStatsBossesKilled = true;
    boolean slayerStatsBossesPerHour = true;
    boolean slayerStatsAverageKillTime = true;
    boolean slayerStatsSessionTime = true;
    boolean slayerHighlightsEnabled;
    boolean slayerHighlightsOnlyMine = true;
    boolean slayerHighlightsBoss = true;
    boolean slayerHighlightsMiniboss = true;
    boolean slayerHighlightsDemon = true;
    boolean slayerHighlightsDepth = true;
    int slayerHighlightsBossColor = 0xFFFF3030;
    int slayerHighlightsMinibossColor = 0xFFFF7A86;
    int slayerHighlightsDemonColor = 0xFFFFAA00;
    double slayerHighlightsBossWidth = 2.0D;
    double slayerHighlightsMinibossWidth = 2.0D;
    double slayerHighlightsDemonWidth = 2.0D;
    boolean slayerHighlightsTargetLines;
    double slayerHighlightsTargetLineWidth = 2.0D;
    double slayerHighlightsTargetLineDistance = 32.0D;
    boolean slayerHighlightsHideSpawnParticles = true;
    boolean slayerHighlightsHideDamageSplash = true;
    boolean slayerHighlightsHideMobNames = true;
    boolean slayerActiveBossTransparencyEnabled;
    int slayerActiveBossTransparencyStrength = 35;
    boolean slayerActiveBossTransparencyPlayers;
    boolean slayerIrrelevantMobsEnabled;
    int slayerIrrelevantMobsStrength = 40;
    boolean slayerMinibossAlertEnabled;
    boolean slayerMinibossAlertMessage = true;
    boolean slayerMinibossAlertTitle = true;
    double slayerMinibossAlertDistance = 10.0D;
    String slayerMinibossAlertText = "Miniboss spawned!";
    String slayerBigMinibossAlertText = "Big slayer miniboss spawned!";
    boolean slayerDropsEnabled;
    boolean slayerDropsShowChance = true;
    boolean slayerDropsBossesSince = true;
    boolean slayerDropsDetectAutomatically = true;
    boolean slayerDropsRngHud;
    boolean slayerDropsRngWarnEmpty = true;
    boolean slayerDropsRngHideChat;
    boolean slayerDropsProfitHud;
    boolean slayerDropsProfitTable = true;
    int slayerDropsProfitItemsShown = 5;
    boolean slayerDropsProfitPerHour = true;
    boolean slayerDropsProfitHideOutsideInventory;
    boolean slayerDropsGroundHighlight;
    boolean slayerDropsGroundLabels;
    int slayerDropsGroundLabelMinimum = 1_000_000;
    boolean slayerDropsPriceInChat;
    boolean slayerDropsPriceTitle;
    boolean slayerDropsPriceTitleSound = true;
    int slayerDropsPriceTitleMinimum = 1_000_000;
    boolean slayerDropsRecentHighlight = true;
    java.util.List<String> slayerDropFilter = new java.util.ArrayList<>();
    java.util.Map<String, String> slayerRngMeterSelectedByFamily = new java.util.LinkedHashMap<>();
    java.util.Map<String, Long> slayerRngMeterStoredXpByFamily = new java.util.LinkedHashMap<>();
    boolean slayerCarryEnabled;
    boolean slayerCarryAnnounceParty = true;
    boolean slayerCarryShowSpawnMessage = true;
    boolean slayerCarryDisplay = true;
    boolean slayerCarryWebhook;
    boolean slayerCarryWebhookEach = true;
    String slayerCarryWebhookUrl = "";
    String slayerCarryVoidT3Prices = "0.8, 0.65";
    String slayerCarryVoidT4Prices = "1.3, 2.3, 2, 1.5";
    String slayerCarryInfernoT2Prices = "2, 1.7, 1.2";
    String slayerCarryInfernoT3Prices = "3.5, 3, 2.5";
    String slayerCarryInfernoT4Prices = "7, 6, 5";
    java.util.List<SlayerCarryPolicy.HistoryEntry> slayerCarryHistory = new java.util.ArrayList<>();
    boolean slayerCocoonAlertEnabled;
    boolean slayerCocoonShowAlert = true;
    String slayerCocoonAlertMessage = "<red>Boss cocooned!";
    String slayerCocoonAlertSound = "block.note_block.pling";
    double slayerCocoonAlertPitch = 1.0D;
    double slayerCocoonAlertVolume = 1.0D;
    boolean slayerCocoonTimer = true;
    boolean slayerDaggerSwapEnabled;
    int slayerDaggerSwapDelay = 1;
    int slayerDaggerSwapVariance = 2;
    boolean slayerLaserHiderEnabled;
    boolean slayerLaserShowForCarries = true;
    boolean slayerAttunementDisplayEnabled;
    boolean slayerAttunementDisplayCount;
    boolean slayerAutoSoulcryEnabled;
    boolean slayerAutoSoulcryCheckMana = true;
    boolean slayerAutoSoulcryCheckHitbox = true;
    boolean slayerAutoSoulcryTickBased = true;
    boolean slayerAutoSoulcryAttackBased;
    boolean slayerAutoSoulcryOtherBosses;
    int slayerAutoSoulcryMinDelay = 1;
    int slayerAutoSoulcryMaxDelay = 3;
    boolean slayerSoundsEnabled;
    boolean slayerSoundsDisableVoidgloom = true;
    boolean slayerSoundsDisableVampire = true;
    boolean slayerVoidgloomEnabled;
    boolean slayerVoidgloomHighlightBeacon = true;
    boolean slayerVoidgloomBeaconWarning = true;
    boolean slayerVoidgloomBeaconSound = true;
    boolean slayerVoidgloomBeaconLine = true;
    boolean slayerVoidgloomBeaconPath = true;
    boolean slayerVoidgloomBeaconTimer = true;
    int slayerVoidgloomBeaconColor = 0xFFFF2020;
    int slayerVoidgloomLineColor = 0xFFFF0058;
    int slayerVoidgloomLineWidth = SlayerFightPolicy.DEFAULT_LINE_WIDTH;
    boolean slayerVoidgloomHighlightHeld = true;
    boolean slayerVoidgloomHighlightNukekubi = true;
    boolean slayerVoidgloomNukekubiLine = true;
    int slayerVoidgloomNukekubiColor = 0xFFFF55FF;
    boolean slayerVoidgloomWorldLabels = true;
    boolean slayerVoidgloomLineToBoss = true;
    int slayerVoidgloomBossLineWidth = SlayerFightPolicy.DEFAULT_LINE_WIDTH;
    boolean slayerVoidgloomPhaseDisplay = true;
    boolean slayerVoidgloomHitsDisplay = true;
    boolean slayerVoidgloomLaserTimer = true;
    boolean slayerVoidgloomLaserHealth = true;
    boolean slayerVoidgloomHideParticles = true;
    boolean slayerRevenantEnabled;
    boolean slayerRevenantBoomDisplay = true;
    boolean slayerRevenantBoomSound = true;
    boolean slayerRevenantBoomHighlight = true;
    int slayerRevenantBoomColor = 0xFFFF2020;
    boolean slayerRevenantWorldLabels = true;
    boolean slayerRevenantLineToBoss = true;
    boolean slayerTarantulaEnabled;
    boolean slayerTarantulaHighlightEggSacs = true;
    int slayerTarantulaEggColor = 0xFFFFFF00;
    boolean slayerTarantulaEggHits = true;
    boolean slayerTarantulaHighlightInvincible = true;
    int slayerTarantulaInvincibleColor = 0xFFAAAAAA;
    boolean slayerTarantulaInvincibleText = true;
    boolean slayerTarantulaPhaseDisplay = true;
    boolean slayerTarantulaWorldLabels = true;
    boolean slayerTarantulaMuteSounds = true;
    boolean slayerTarantulaLineToBoss = true;
    int slayerTarantulaBossLineWidth = SlayerFightPolicy.DEFAULT_LINE_WIDTH;
    boolean slayerSvenEnabled;
    boolean slayerSvenHighlightPups = true;
    int slayerSvenPupColor = 0xFF55FFFF;
    boolean slayerSvenPupLine = true;
    boolean slayerSvenWorldLabels = true;
    boolean slayerSvenHidePupNametags = true;
    boolean slayerSvenHowlWarning = true;
    boolean slayerSvenMuteSounds = true;
    boolean slayerSvenLineToBoss = true;
    boolean slayerVampireMarkersEnabled;
    boolean slayerVampireMarkersBloodIchor = true;
    int slayerVampireMarkersIchorColor = 0xFFFF0044;
    boolean slayerVampireMarkersKillerSpring = true;
    int slayerVampireMarkersSpringColor = 0xFFAA00FF;
    boolean slayerVampireMarkersTwinclaws = true;
    int slayerVampireMarkersTwinclawsDelay = 0;
    boolean slayerVampireMarkersMania = true;
    boolean slayerVampireMarkersManiaTimer = true;
    boolean slayerVampireMarkersSteakAlert = true;
    int slayerVampireMarkersSteakColor = 0xFFFF0058;
    boolean slayerVampireMarkersWorldLabels = true;
    boolean slayerVampireMarkersMuteSounds = true;
    boolean slayerVampireMarkersLineToBoss = true;
    int slayerVampireMarkersBossLineWidth = SlayerFightPolicy.DEFAULT_LINE_WIDTH;
    boolean slayerVampireMarkersIchorBeam = true;
    boolean slayerVampireMarkersChalice = true;
    int slayerVampireMarkersChaliceColor = 0xFF55FFFF;
    boolean slayerVampireMarkersEffigies = true;
    boolean slayerInfernoEnabled;
    boolean slayerInfernoFirePillar = true;
    boolean slayerInfernoFirePillarSound = true;
    int slayerInfernoPillarColor = 0xFFFF5500;
    boolean slayerInfernoFirePits = true;
    boolean slayerInfernoPhaseDisplay = true;
    boolean slayerInfernoColorByAttunement = true;
    boolean slayerInfernoHideChat = true;
    boolean slayerInfernoHideParticles = true;
    boolean slayerInfernoWorldLabels = true;
    boolean slayerInfernoLineToBoss = true;
    boolean slayerInfernoGummyWarning = true;
    boolean slayerQuestWarningEnabled;
    boolean slayerQuestWarningTitle = true;
    boolean slayerQuestWarningChat = true;
    boolean slayerAutoStartEnabled;
    int slayerAutoStartDelay = 8;
    boolean slayerAutoStartBlockNotSpawnable = true;
    boolean slayerVengeanceEnabled;
    boolean slayerVengeanceCompact;
    boolean slayerVengeanceUseTicks = true;
    boolean slayerVengeanceDamageEnabled;
    boolean slayerVengeanceDamageAbbreviate = true;
    boolean slayerBigDropsEnabled;
    double slayerBigDropsScale = 3.0D;
    double slayerBigDropsRange = 1.0D;
    int slayerBigDropsUnscaleSeconds = 15;
    boolean slayerBigDropsRevenant = true;
    boolean slayerBigDropsTarantula = true;
    boolean slayerBigDropsSven = true;
    boolean slayerBigDropsVoidgloom = true;
    boolean slayerBigDropsInferno = true;
    boolean slayerBigDropsVampire = true;

    boolean storageOverlayEnabled;
    boolean storageOverlayAlwaysOpen = true;
    boolean storageOverlayOutlineActive = true;
    int storageOverlayOutlineColor = 0xFFFFFF00;
    boolean storageOverlayInactiveTooltips = true;
    int storageOverlayColumns = 3;
    int storageOverlayHeight = 324;
    boolean storageOverlayRetainScroll = true;
    int storageOverlayScrollSpeed = 10;
    boolean storageOverlayInvertScroll;
    int storageOverlayPadding = 5;
    int storageOverlayMargin = 20;
    boolean storageOverlayBlockItemScroll = true;
    boolean storageOverlayHighlightSearch = true;
    boolean storageOverlayFilterSearch;
    int storageOverlayHighlightColor = 0x8000C000;
    int storageOverlayPanelColor = 0xF00A1520;
    int storageOverlayCardColor = 0xFF122433;
    int storageOverlayCardActiveColor = 0xFF18384A;
    int storageOverlayPlayerColor = 0xF00A1520;
    String storageOverlaySearchQuery = "";
    boolean storageItemSearch = true;
    boolean storageCraftHelper = true;
    boolean storageMuseumArmor = true;
    String storageItemSearchKeybind = "";

    boolean inventoryButtonsEnabled;
    boolean inventoryButtonsHoverTooltip = true;
    boolean inventoryButtonsInventoryOnly;
    java.util.List<InventoryButtonsPolicy.Button> inventoryButtons =
            new java.util.ArrayList<>(InventoryButtonsPolicy.simplePreset());
    java.util.List<InventoryButtonsPolicy.Button> inventoryButtonsSavedPreset = new java.util.ArrayList<>();

    boolean rewardClaimEnabled;
    boolean rewardClaimMuteChatLink = true;
    boolean rewardClaimBlockBrowser = true;
    boolean rewardClaimWaitForAd = true;

    boolean hideEmptyTooltips;
    boolean hideBreakParticles;
    boolean hideBossBar;
    boolean hideArmorBar;
    boolean hideFoodBar;
    boolean hideFog;
    boolean hideEffectDisplay;
    boolean hideRecipeBook;
    boolean hideSelectedItemName;
    boolean hideDeadEntities;
    boolean hideDeadPoof;
    boolean hideImplosionParticles;
    boolean hideEntityFire;
    boolean hideMageBeam;
    boolean hideIceSpray;
    boolean hidePowderCoating;
    boolean hideGuidedSheep;
    boolean hideBonePlating;
    boolean hideTreeBits;
    boolean hideNausea;
    boolean hideStuckArrows;
    String vignetteMode = VIGNETTE_NONE;
    String hideIslandClouds = SkyBlockUtilityPolicy.CLOUD_OFF;
    boolean netherFog = true;
    double netherFogScale = SkyBlockUtilityPolicy.DEFAULT_NETHER_FOG_SCALE;
    boolean totemAnimation = true;
    boolean mobIcons;
    int armorSelf = SkyBlockUtilityPolicy.DEFAULT_ARMOR_PERCENT;
    int armorOthers = SkyBlockUtilityPolicy.DEFAULT_ARMOR_PERCENT;

    boolean freecamEnabled;
    double freecamSpeed = FreecamPolicy.DEFAULT_SPEED;
    boolean freecamShowBody = true;
    boolean freecamCollide;
    String freecamKeybind = "";

    boolean farmKeysEnabled;
    boolean farmKeysLockCamera = true;
    String farmKeysAttack = "";
    String farmKeysJump = "";
    String farmKeysPrevAttack = "";
    String farmKeysPrevJump = "";

    boolean hudLayoutEnabled;
    boolean hudLayoutShowBackground = true;
    boolean hudLayoutDimUnfocused = true;
    boolean hudHideHotbar;
    boolean hudHideHealth;
    boolean hudHideFood;
    boolean hudHideArmor;
    boolean hudHideXp;
    boolean hudHideAir;
    boolean hudHideMount;
    boolean hudHideScoreboard;
    boolean hudHideBoss;
    boolean hudHideAction;
    boolean hudHideItemName;
    boolean hudHideEffects;
    boolean hudHideTitles;
    boolean hudHideTab;
    int hudLayoutBackgroundColor = HudStylePolicy.DEFAULT_BG;
    int hudLayoutTextColor = HudStylePolicy.DEFAULT_TEXT;
    float hudLayoutScale = HudStylePolicy.DEFAULT_SCALE;
    Map<String, HudStyleState> hudStyles = new LinkedHashMap<>();
    int hudEditorTitleX = HudEditorChromePolicy.UNPLACED;
    int hudEditorTitleY = HudEditorChromePolicy.UNPLACED;
    int hudEditorHelpX = HudEditorChromePolicy.UNPLACED;
    int hudEditorHelpY = HudEditorChromePolicy.UNPLACED;
    int hudEditorInspectorX = HudEditorChromePolicy.UNPLACED;
    int hudEditorInspectorY = HudEditorChromePolicy.UNPLACED;

    boolean customCursorEnabled;
    double customCursorSize = CustomCursorPolicy.DEFAULT_SIZE;
    int customCursorFill = CustomCursorPolicy.DEFAULT_FILL;
    int customCursorOutline = CustomCursorPolicy.DEFAULT_OUTLINE;
    int customCursorAccent = CustomCursorPolicy.DEFAULT_ACCENT;
    boolean customCursorClickAnim = true;
    boolean customCursorHoldAnim = true;
    boolean customCursorHideVanilla = true;

    boolean legacyTexturesEnabled;
    boolean legacyTexturesItems = true;

    boolean customResourcePackEnabled;
    boolean customResourcePackOverworld = true;
    boolean customResourcePackCrimson = true;
    boolean customResourcePackEnd = true;
    boolean customResourcePackGameplayFont;
    boolean fullTextShadow;

    boolean iotaAddonsEnabled;
    boolean iotaPartyJoinSound = true;
    boolean iotaLimboAlert = true;
    boolean iotaFixFishingHook = true;
    boolean iotaMuteFishingCast = true;
    boolean iotaMuteTerminator = true;
    boolean iotaPartyCommands = true;
    boolean iotaPartyWarp = true;
    boolean iotaPartyTransfer = true;
    boolean iotaPartyPing = true;
    boolean iotaPartyAllInvite = true;
    boolean iotaPartyTps = true;
    boolean iotaPartyPromote = true;
    boolean iotaPartyKick = true;
    boolean iotaPartyKuudra = true;
    boolean iotaPartyChests = true;
    boolean iotaPartyRuns = true;
    boolean iotaPartyProfit = true;
    boolean iotaArrowTracker = true;
    boolean iotaArrowNotifications = true;
    String iotaArrowVisibility = IotaPolicy.VISIBILITY_ALWAYS;
    boolean iotaAutoRequeue;
    String iotaToggleLeftKeybind = "";
    String iotaToggleRightKeybind = "";
    boolean iotaSupplyWaypoints = true;
    boolean iotaSupplyHitbox = true;
    boolean iotaSupplyPullCircle = true;
    boolean iotaSupplyGiantHitbox = true;
    boolean iotaPileWaypoints = true;
    boolean iotaPileNames = true;
    boolean iotaPearlWaypoints = true;
    boolean iotaBuildWaypoints = true;
    boolean iotaStunWaypoints = true;
    String iotaStunPod = IotaKuudraPolicy.STUN_LEFT;
    boolean iotaIchorPool = true;
    boolean iotaKuudraHitbox = true;
    boolean iotaEtherwarpHelper = true;
    boolean iotaFreshTools;
    boolean iotaFreshAnnounce;
    boolean iotaFreshParty;
    boolean iotaBuildInfo;
    boolean iotaKuudraTitles;
    float iotaArrowHudX = 12.0F;
    float iotaArrowHudY = 48.0F;
    float iotaAlertHudX = 12.0F;
    float iotaAlertHudY = 120.0F;
    int iotaChestCount;
    int iotaRunCount;
    int iotaFailedRunCount;
    double iotaAverageRunSeconds;
    long iotaProfitCoins;
    long iotaHourlyRateCoins;
    long iotaSessionDurationMs;
    long iotaLastActivityAt;
    long iotaLastSessionWarningAt;
    String iotaLastKuudraInstance = "KUUDRA_NORMAL";

    boolean stallMarketEnabled;
    boolean stallBazaarSearch = true;
    boolean stallSellProtection = true;
    long stallSellThreshold = StallMarketPolicy.DEFAULT_SELL_THRESHOLD;
    boolean stallAngryCoop = true;
    boolean stallBinOverlay = true;
    boolean stallAhHighlight = true;
    String stallSearchKeybind = "";
    float stallBinHudX = 12.0F;
    float stallBinHudY = 64.0F;

    boolean isModuleEnabled(String moduleId) {
        return switch (moduleId) {
            case "qol.info_tooltips" -> infoTooltipsEnabled;
            case "qol.price_tooltips" -> priceTooltipsEnabled;
            case "qol.viewmodel" -> viewmodelEnabled;
            case "qol.animation_fix" -> animationFixEnabled;
            case "qol.disconnect_fix" -> disconnectFixEnabled;
            case "qol.double_use_fix" -> doubleUseFixEnabled;
            case "qol.eye_height_fix" -> eyeHeightFixEnabled;
            case "qol.instant_sneak" -> instantSneakEnabled;
            case "qol.item_count_fix" -> itemCountFixEnabled;
            case "qol.item_scale" -> itemScaleEnabled;
            case "qol.active_pet_highlight" -> activePetHighlightEnabled;
            case "qol.anvil_helper" -> anvilHelperEnabled;
            case "qol.calendar_date" -> calendarDateEnabled;
            case "qol.experiment_solver" -> experimentSolverEnabled;
            case "qol.auto_experiments" -> autoExperimentsEnabled;
            case "qol.cheater_wardrobe" -> cheaterWardrobeEnabled;
            case "qol.escrow_fix" -> escrowFixEnabled;
            case "qol.auto_harp" -> autoHarpEnabled;
            case "qol.auto_gfs" -> autoGfsEnabled;
            case "qol.auto_sell" -> autoSellEnabled;
            case "qol.ghosts" -> ghostsEnabled;
            case "qol.auto_dojo" -> autoDojoEnabled;
            case "qol.fishing_creatures" -> fishingCreaturesEnabled;
            case "qol.fishing_hotspots" -> fishingHotspotsEnabled;
            case "qol.fishing_trophy" -> fishingTrophyEnabled;
            case "qol.fishing_visuals" -> fishingVisualsEnabled;
            case "qol.fishing_tools" -> fishingToolsEnabled;
            case "qol.mining_scatha" -> miningScathaEnabled;
            case "qol.mining_events" -> miningEventsEnabled;
            case "qol.mining_glacite" -> miningGlaciteEnabled;
            case "qol.mining_helpers" -> miningHelpersEnabled;
            case "qol.mining_hotm" -> miningHotmEnabled;
            case "qol.diana_burrows" -> dianaBurrowsEnabled;
            case "qol.diana_mobs" -> dianaMobsEnabled;
            case "qol.diana_profit" -> dianaProfitEnabled;
            case "qol.diana_share" -> dianaShareEnabled;
            case "qol.foraging_trees" -> foragingTreesEnabled;
            case "qol.foraging_audio" -> foragingAudioEnabled;
            case "qol.foraging_helpers" -> foragingHelpersEnabled;
            case "qol.foraging_cheats" -> foragingCheatsEnabled;
            case "qol.dungeon_hud" -> dungeonHudEnabled;
            case "qol.dungeon_esp" -> dungeonEspEnabled;
            case "qol.dungeon_announce" -> dungeonAnnounceEnabled;
            case "qol.dungeon_leap" -> dungeonLeapEnabled;
            case "qol.dungeon_terminals" -> dungeonTerminalsEnabled;
            case "qol.dungeon_termsim" -> dungeonTermSimEnabled;
            case "qol.dungeon_requeue" -> dungeonRequeueEnabled;
            case "qol.dungeon_puzzles" -> dungeonPuzzlesEnabled;
            case "qol.dungeon_f7" -> dungeonF7Enabled;
            case "qol.dungeon_menus" -> dungeonMenusEnabled;
            case "qol.farm_keys" -> farmKeysEnabled;
            case "qol.slayer_display" -> slayerDisplayEnabled;
            case "qol.slayer_time_messages" -> slayerTimeMessagesEnabled;
            case "qol.slayer_progress" -> slayerProgressEnabled;
            case "qol.slayer_stats" -> slayerStatsEnabled;
            case "qol.slayer_highlights" -> slayerHighlightsEnabled;
            case "qol.slayer_active_boss_transparency" -> slayerActiveBossTransparencyEnabled;
            case "qol.slayer_irrelevant_mobs" -> slayerIrrelevantMobsEnabled;
            case "qol.slayer_miniboss_alert" -> slayerMinibossAlertEnabled;
            case "qol.slayer_drops" -> slayerDropsEnabled;
            case "qol.slayer_carry" -> slayerCarryEnabled;
            case "qol.slayer_cocoon_alert" -> slayerCocoonAlertEnabled;
            case "qol.slayer_dagger_swap" -> slayerDaggerSwapEnabled;
            case "qol.slayer_laser_hider" -> slayerLaserHiderEnabled;
            case "qol.slayer_attunement_display" -> slayerAttunementDisplayEnabled;
            case "qol.slayer_auto_soulcry" -> slayerAutoSoulcryEnabled;
            case "qol.slayer_sounds" -> slayerSoundsEnabled;
            case "qol.slayer_voidgloom" -> slayerVoidgloomEnabled;
            case "qol.slayer_revenant" -> slayerRevenantEnabled;
            case "qol.slayer_tarantula" -> slayerTarantulaEnabled;
            case "qol.slayer_sven" -> slayerSvenEnabled;
            case "qol.slayer_vampire_markers" -> slayerVampireMarkersEnabled;
            case "qol.slayer_inferno" -> slayerInfernoEnabled;
            case "qol.slayer_quest_warning" -> slayerQuestWarningEnabled;
            case "qol.slayer_auto_start" -> slayerAutoStartEnabled;
            case "qol.slayer_vengeance" -> slayerVengeanceEnabled;
            case "qol.slayer_vengeance_damage" -> slayerVengeanceDamageEnabled;
            case "qol.slayer_big_drops" -> slayerBigDropsEnabled;
            case "qol.storage_overlay" -> storageOverlayEnabled;
            case "qol.inventory_buttons" -> inventoryButtonsEnabled;
            case "qol.reward_claim" -> rewardClaimEnabled;
            case "qol.freecam" -> freecamEnabled;
            case "qol.hud_layout" -> hudLayoutEnabled;
            case "qol.custom_cursor" -> customCursorEnabled;
            case "qol.legacy_textures" -> legacyTexturesEnabled;
            case "qol.custom_resource_pack" -> customResourcePackEnabled;
            case "qol.iota" -> iotaAddonsEnabled;
            case "qol.stall_market" -> stallMarketEnabled;
            case "qol.custom_scoreboard" -> board().enabled;
            default -> Boolean.TRUE.equals(athen().readBoolean(moduleId));
        };
    }

    boolean setModuleEnabled(String moduleId, boolean enabled) {
        switch (moduleId) {
            case "qol.info_tooltips" -> infoTooltipsEnabled = enabled;
            case "qol.price_tooltips" -> priceTooltipsEnabled = enabled;
            case "qol.viewmodel" -> viewmodelEnabled = enabled;
            case "qol.animation_fix" -> animationFixEnabled = enabled;
            case "qol.disconnect_fix" -> disconnectFixEnabled = enabled;
            case "qol.double_use_fix" -> doubleUseFixEnabled = enabled;
            case "qol.eye_height_fix" -> eyeHeightFixEnabled = enabled;
            case "qol.instant_sneak" -> instantSneakEnabled = enabled;
            case "qol.item_count_fix" -> itemCountFixEnabled = enabled;
            case "qol.item_scale" -> itemScaleEnabled = enabled;
            case "qol.active_pet_highlight" -> activePetHighlightEnabled = enabled;
            case "qol.anvil_helper" -> anvilHelperEnabled = enabled;
            case "qol.calendar_date" -> calendarDateEnabled = enabled;
            case "qol.experiment_solver" -> experimentSolverEnabled = enabled;
            case "qol.auto_experiments" -> autoExperimentsEnabled = enabled;
            case "qol.cheater_wardrobe" -> cheaterWardrobeEnabled = enabled;
            case "qol.escrow_fix" -> escrowFixEnabled = enabled;
            case "qol.auto_harp" -> autoHarpEnabled = enabled;
            case "qol.auto_gfs" -> autoGfsEnabled = enabled;
            case "qol.auto_sell" -> autoSellEnabled = enabled;
            case "qol.ghosts" -> ghostsEnabled = enabled;
            case "qol.auto_dojo" -> autoDojoEnabled = enabled;
            case "qol.fishing_creatures" -> fishingCreaturesEnabled = enabled;
            case "qol.fishing_hotspots" -> fishingHotspotsEnabled = enabled;
            case "qol.fishing_trophy" -> fishingTrophyEnabled = enabled;
            case "qol.fishing_visuals" -> fishingVisualsEnabled = enabled;
            case "qol.fishing_tools" -> fishingToolsEnabled = enabled;
            case "qol.mining_scatha" -> miningScathaEnabled = enabled;
            case "qol.mining_events" -> miningEventsEnabled = enabled;
            case "qol.mining_glacite" -> miningGlaciteEnabled = enabled;
            case "qol.mining_helpers" -> miningHelpersEnabled = enabled;
            case "qol.mining_hotm" -> miningHotmEnabled = enabled;
            case "qol.diana_burrows" -> dianaBurrowsEnabled = enabled;
            case "qol.diana_mobs" -> dianaMobsEnabled = enabled;
            case "qol.diana_profit" -> dianaProfitEnabled = enabled;
            case "qol.diana_share" -> dianaShareEnabled = enabled;
            case "qol.foraging_trees" -> foragingTreesEnabled = enabled;
            case "qol.foraging_audio" -> foragingAudioEnabled = enabled;
            case "qol.foraging_helpers" -> foragingHelpersEnabled = enabled;
            case "qol.foraging_cheats" -> foragingCheatsEnabled = enabled;
            case "qol.dungeon_hud" -> dungeonHudEnabled = enabled;
            case "qol.dungeon_esp" -> dungeonEspEnabled = enabled;
            case "qol.dungeon_announce" -> dungeonAnnounceEnabled = enabled;
            case "qol.dungeon_leap" -> dungeonLeapEnabled = enabled;
            case "qol.dungeon_terminals" -> dungeonTerminalsEnabled = enabled;
            case "qol.dungeon_termsim" -> dungeonTermSimEnabled = enabled;
            case "qol.dungeon_requeue" -> dungeonRequeueEnabled = enabled;
            case "qol.dungeon_puzzles" -> dungeonPuzzlesEnabled = enabled;
            case "qol.dungeon_f7" -> dungeonF7Enabled = enabled;
            case "qol.dungeon_menus" -> dungeonMenusEnabled = enabled;
            case "qol.farm_keys" -> farmKeysEnabled = enabled;
            case "qol.slayer_display" -> slayerDisplayEnabled = enabled;
            case "qol.slayer_time_messages" -> slayerTimeMessagesEnabled = enabled;
            case "qol.slayer_progress" -> slayerProgressEnabled = enabled;
            case "qol.slayer_stats" -> slayerStatsEnabled = enabled;
            case "qol.slayer_highlights" -> slayerHighlightsEnabled = enabled;
            case "qol.slayer_active_boss_transparency" -> slayerActiveBossTransparencyEnabled = enabled;
            case "qol.slayer_irrelevant_mobs" -> slayerIrrelevantMobsEnabled = enabled;
            case "qol.slayer_miniboss_alert" -> slayerMinibossAlertEnabled = enabled;
            case "qol.slayer_drops" -> slayerDropsEnabled = enabled;
            case "qol.slayer_carry" -> slayerCarryEnabled = enabled;
            case "qol.slayer_cocoon_alert" -> slayerCocoonAlertEnabled = enabled;
            case "qol.slayer_dagger_swap" -> slayerDaggerSwapEnabled = enabled;
            case "qol.slayer_laser_hider" -> slayerLaserHiderEnabled = enabled;
            case "qol.slayer_attunement_display" -> slayerAttunementDisplayEnabled = enabled;
            case "qol.slayer_auto_soulcry" -> slayerAutoSoulcryEnabled = enabled;
            case "qol.slayer_sounds" -> slayerSoundsEnabled = enabled;
            case "qol.slayer_voidgloom" -> slayerVoidgloomEnabled = enabled;
            case "qol.slayer_revenant" -> slayerRevenantEnabled = enabled;
            case "qol.slayer_tarantula" -> slayerTarantulaEnabled = enabled;
            case "qol.slayer_sven" -> slayerSvenEnabled = enabled;
            case "qol.slayer_vampire_markers" -> slayerVampireMarkersEnabled = enabled;
            case "qol.slayer_inferno" -> slayerInfernoEnabled = enabled;
            case "qol.slayer_quest_warning" -> slayerQuestWarningEnabled = enabled;
            case "qol.slayer_auto_start" -> slayerAutoStartEnabled = enabled;
            case "qol.slayer_vengeance" -> slayerVengeanceEnabled = enabled;
            case "qol.slayer_vengeance_damage" -> slayerVengeanceDamageEnabled = enabled;
            case "qol.slayer_big_drops" -> slayerBigDropsEnabled = enabled;
            case "qol.storage_overlay" -> storageOverlayEnabled = enabled;
            case "qol.inventory_buttons" -> inventoryButtonsEnabled = enabled;
            case "qol.reward_claim" -> rewardClaimEnabled = enabled;
            case "qol.freecam" -> freecamEnabled = enabled;
            case "qol.hud_layout" -> hudLayoutEnabled = enabled;
            case "qol.custom_cursor" -> customCursorEnabled = enabled;
            case "qol.legacy_textures" -> legacyTexturesEnabled = enabled;
            case "qol.custom_resource_pack" -> customResourcePackEnabled = enabled;
            case "qol.iota" -> iotaAddonsEnabled = enabled;
            case "qol.stall_market" -> stallMarketEnabled = enabled;
            case "qol.custom_scoreboard" -> board().enabled = enabled;
            default -> {
                if (!athen().setModuleEnabled(moduleId, enabled)) {
                    return false;
                }
            }
        }
        return true;
    }

    Boolean readBoolean(String settingId) {
        if (settingId != null && settingId.startsWith("qol.custom_scoreboard.")) {
            Boolean value = board().readBoolean(settingId);
            if (value != null) {
                return value;
            }
        }
        return switch (settingId) {
            case "qol.animation_fix.dyes" -> animationDyes;
            case "qol.animation_fix.skins" -> animationSkins;
            case "qol.info_tooltips.dungeon_quality" -> infoDungeonQuality;
            case "qol.info_tooltips.created_date" -> infoCreatedDate;
            case "qol.info_tooltips.hex_color" -> infoHexColor;
            case "qol.info_tooltips.museum" -> infoMuseum;
            case "qol.info_tooltips.item_id" -> infoItemId;
            case "qol.info_tooltips.star_count" -> infoStarCount;
            case "qol.info_tooltips.pet_candy" -> infoPetCandy;
            case "qol.info_tooltips.revert_master_stars" -> infoRevertMasterStars;
            case "qol.calendar_date.minister" -> calendarMinister;
            case "qol.price_tooltips.lowest_bin" -> priceLowestBin;
            case "qol.price_tooltips.bazaar" -> priceBazaar;
            case "qol.price_tooltips.npc" -> priceNpc;
            case "qol.price_tooltips.motes" -> priceMotes;
            case "qol.price_tooltips.price_paid" -> pricePaid;
            case "qol.viewmodel.no_haste" -> viewmodelNoHaste;
            case "qol.viewmodel.no_equip" -> viewmodelNoEquip;
            case "qol.viewmodel.no_bow_swing" -> viewmodelNoBowSwing;
            case "qol.viewmodel.apply_to_hand" -> viewmodelApplyToHand;
            case "qol.render_optimizer.hide_empty_tooltips" -> hideEmptyTooltips;
            case "qol.render_optimizer.hide_break_particles" -> hideBreakParticles;
            case "qol.render_optimizer.hide_boss_bar" -> hideBossBar;
            case "qol.render_optimizer.hide_armor_bar" -> hideArmorBar;
            case "qol.render_optimizer.hide_food_bar" -> hideFoodBar;
            case "qol.render_optimizer.hide_fog" -> hideFog;
            case "qol.render_optimizer.hide_effect_display" -> hideEffectDisplay;
            case "qol.render_optimizer.hide_recipe_book" -> hideRecipeBook;
            case "qol.render_optimizer.hide_selected_item_name" -> hideSelectedItemName;
            case "qol.render_optimizer.hide_dead_entities" -> hideDeadEntities;
            case "qol.render_optimizer.hide_dead_poof" -> hideDeadPoof;
            case "qol.render_optimizer.hide_implosion_particles" -> hideImplosionParticles;
            case "qol.render_optimizer.hide_entity_fire" -> hideEntityFire;
            case "qol.render_optimizer.hide_mage_beam" -> hideMageBeam;
            case "qol.render_optimizer.hide_ice_spray" -> hideIceSpray;
            case "qol.render_optimizer.hide_powder_coating" -> hidePowderCoating;
            case "qol.render_optimizer.hide_guided_sheep" -> hideGuidedSheep;
            case "qol.render_optimizer.hide_bone_plating" -> hideBonePlating;
            case "qol.render_optimizer.hide_tree_bits" -> hideTreeBits;
            case "qol.render_optimizer.hide_nausea" -> hideNausea;
            case "qol.render_optimizer.hide_stuck_arrows" -> hideStuckArrows;
            case "qol.render_optimizer.nether_fog" -> netherFog;
            case "qol.render_optimizer.totem_animation" -> totemAnimation;
            case "qol.render_optimizer.mob_icons" -> mobIcons;
            case "qol.experiment_solver.chronomatron" -> experimentChronomatron;
            case "qol.experiment_solver.ultrasequencer" -> experimentUltrasequencer;
            case "qol.experiment_solver.superpairs" -> experimentSuperpairs;
            case "qol.experiment_solver.block_wrong_clicks" -> experimentBlockWrongClicks;
            case "qol.experiment_solver.hide_tooltip" -> experimentHideTooltip;
            case "qol.experiment_solver.hide_wrong_chronomatron" -> experimentHideWrongChrono;
            case "qol.experiment_solver.hide_wrong_ultrasequencer" -> experimentHideWrongUltra;
            case "qol.experiment_solver.private_island_only" -> experimentPrivateIslandOnly;
            case "qol.auto_experiments.auto_close" -> autoExperimentsAutoClose;
            case "qol.auto_experiments.get_max_xp" -> autoExperimentsGetMaxXp;
            case "qol.cheater_wardrobe.move_equip" -> cheaterWardrobeMoveEquip;
            case "qol.cheater_wardrobe.stationary_only" -> cheaterWardrobeStationaryOnly;
            case "qol.cheater_wardrobe.reset_open" -> cheaterWardrobeResetOpen;
            case "qol.auto_gfs.in_skyblock" -> autoGfsInSkyblock;
            case "qol.auto_gfs.in_kuudra" -> autoGfsInKuudra;
            case "qol.auto_gfs.in_dungeon" -> autoGfsInDungeon;
            case "qol.auto_gfs.refill_on_dungeon_start" -> autoGfsRefillOnDungeonStart;
            case "qol.auto_gfs.refill_on_timer" -> autoGfsRefillOnTimer;
            case "qol.auto_gfs.refill_pearl" -> autoGfsRefillPearl;
            case "qol.auto_gfs.refill_jerry" -> autoGfsRefillJerry;
            case "qol.auto_gfs.refill_tnt" -> autoGfsRefillTnt;
            case "qol.auto_gfs.refill_leap" -> autoGfsRefillLeap;
            case "qol.auto_gfs.refill_twilight" -> autoGfsRefillTwilight;
            case "qol.auto_gfs.auto_get_draft" -> autoGfsAutoGetDraft;
            case "qol.ghosts.show_ghosts" -> ghostsShowGhosts;
            case "qol.ghosts.show_powered" -> ghostsShowPowered;
            case "qol.auto_dojo.control" -> autoDojoControl;
            case "qol.auto_dojo.mastery" -> autoDojoMastery;
            case "qol.auto_dojo.discipline" -> autoDojoDiscipline;
            case "qol.auto_dojo.discipline_attack" -> autoDojoDisciplineAttack;
            case "qol.fishing_creatures.hud" -> fishingCreaturesHud;
            case "qol.fishing_creatures.cap_notify" -> fishingCreaturesCapNotify;
            case "qol.fishing_creatures.timer_notify" -> fishingCreaturesTimerNotify;
            case "qol.fishing_creatures.rare_announce" -> fishingCreaturesRareAnnounce;
            case "qol.fishing_creatures.rare_sound" -> fishingCreaturesRareSound;
            case "qol.fishing_creatures.rare_party" -> fishingCreaturesRareParty;
            case "qol.fishing_creatures.rare_esp" -> fishingCreaturesRareEsp;
            case "qol.fishing_creatures.shorten_chat" -> fishingCreaturesShortenChat;
            case "qol.fishing_creatures.hide_common" -> fishingCreaturesHideCommon;
            case "qol.fishing_creatures.auto_attack" -> fishingCreaturesAutoAttack;
            case "qol.fishing_creatures.thunder_sparks" -> fishingCreaturesThunderSparks;
            case "qol.fishing_hotspots.circle" -> fishingHotspotsCircle;
            case "qol.fishing_hotspots.hide_particles" -> fishingHotspotsHideParticles;
            case "qol.fishing_hotspots.radar" -> fishingHotspotsRadar;
            case "qol.fishing_hotspots.tracer" -> fishingHotspotsTracer;
            case "qol.fishing_hotspots.despawn" -> fishingHotspotsDespawn;
            case "qol.fishing_trophy.titles" -> fishingTrophyTitles;
            case "qol.fishing_trophy.filter_chat" -> fishingTrophyFilterChat;
            case "qol.fishing_trophy.golden_timer" -> fishingTrophyGoldenTimer;
            case "qol.fishing_trophy.geyser" -> fishingTrophyGeyser;
            case "qol.fishing_trophy.sponge" -> fishingTrophySponge;
            case "qol.fishing_trophy.fillet" -> fishingTrophyFillet;
            case "qol.fishing_visuals.hide_other_bobbers" -> fishingVisualsHideOtherBobbers;
            case "qol.fishing_visuals.chum_hider" -> fishingVisualsChumHider;
            case "qol.fishing_visuals.mute_banshee" -> fishingVisualsMuteBanshee;
            case "qol.fishing_visuals.mute_drake" -> fishingVisualsMuteDrake;
            case "qol.fishing_tools.bait_hud" -> fishingToolsBaitHud;
            case "qol.fishing_tools.no_bait_warn" -> fishingToolsNoBaitWarn;
            case "qol.fishing_tools.bait_change" -> fishingToolsBaitChange;
            case "qol.fishing_tools.thunder_notify" -> fishingToolsThunderNotify;
            case "qol.fishing_tools.totem_hud" -> fishingToolsTotemHud;
            case "qol.mining_scatha.titles" -> miningScathaTitles;
            case "qol.mining_scatha.sounds" -> miningScathaSounds;
            case "qol.mining_scatha.cooldown" -> miningScathaCooldown;
            case "qol.mining_scatha.pet_drop" -> miningScathaPetDrop;
            case "qol.mining_scatha.pet_rarity" -> miningScathaPetRarity;
            case "qol.mining_scatha.hud" -> miningScathaHud;
            case "qol.mining_scatha.party" -> miningScathaParty;
            case "qol.mining_events.hud" -> miningEventsHud;
            case "qol.mining_events.titles" -> miningEventsTitles;
            case "qol.mining_events.goblin_esp" -> miningEventsGoblinEsp;
            case "qol.mining_glacite.pity_hud" -> miningGlacitePityHud;
            case "qol.mining_glacite.corpse_hud" -> miningGlaciteCorpseHud;
            case "qol.mining_glacite.cold_overlay" -> miningGlaciteColdOverlay;
            case "qol.mining_glacite.party_share" -> miningGlacitePartyShare;
            case "qol.mining_glacite.shaft_party" -> miningGlaciteShaftParty;
            case "qol.mining_glacite.pity_chat" -> miningGlacitePityChat;
            case "qol.mining_glacite.enter_title" -> miningGlaciteEnterTitle;
            case "qol.mining_glacite.enter_chat" -> miningGlaciteEnterChat;
            case "qol.mining_glacite.enter_party" -> miningGlaciteEnterParty;
            case "qol.mining_glacite.corpse_waypoints" -> miningGlaciteCorpseWaypoints;
            case "qol.mining_glacite.key_announce" -> miningGlaciteKeyAnnounce;
            case "qol.mining_helpers.fetchur" -> miningHelpersFetchur;
            case "qol.mining_helpers.fossil_muncher" -> miningHelpersFossilMuncher;
            case "qol.mining_helpers.drill_fuel" -> miningHelpersDrillFuel;
            case "qol.mining_helpers.ability_hud" -> miningHelpersAbilityHud;
            case "qol.mining_helpers.commission_gui" -> miningHelpersCommissionGui;
            case "qol.mining_helpers.commission_mobs" -> miningHelpersCommissionMobs;
            case "qol.mining_helpers.notify_portal" -> miningHelpersNotifyPortal;
            case "qol.mining_helpers.notify_scrap" -> miningHelpersNotifyScrap;
            case "qol.mining_helpers.notify_goblin" -> miningHelpersNotifyGoblin;
            case "qol.mining_helpers.metal_distance" -> miningHelpersMetalDistance;
            case "qol.mining_helpers.detector_solver" -> miningHelpersDetectorSolver;
            case "qol.mining_helpers.detector_ding" -> miningHelpersDetectorDing;
            case "qol.mining_helpers.detector_title" -> miningHelpersDetectorTitle;
            case "qol.mining_helpers.red_carpets" -> miningHelpersRedCarpets;
            case "qol.mining_helpers.fossil_excavator" -> miningHelpersFossilExcavator;
            case "qol.mining_helpers.wishing_compass" -> miningHelpersWishingCompass;
            case "qol.mining_helpers.call_king" -> miningHelpersCallKing;
            case "qol.mining_helpers.break_reset" -> miningHelpersBreakReset;
            case "qol.mining_helpers.gemstone_desync" -> miningHelpersGemstoneDesync;
            case "qol.mining_hotm.sky_mall" -> miningHotmSkyMall;
            case "qol.mining_hotm.screen_hint" -> miningHotmScreenHint;
            case "qol.diana_burrows.guess" -> dianaBurrowsGuess;
            case "qol.diana_burrows.particles" -> dianaBurrowsParticles;
            case "qol.diana_burrows.waypoints" -> dianaBurrowsWaypoints;
            case "qol.diana_burrows.mute_spade" -> dianaBurrowsMuteSpade;
            case "qol.diana_burrows.fix_chat" -> dianaBurrowsFixChat;
            case "qol.diana_mobs.rare_esp" -> dianaMobsRareEsp;
            case "qol.diana_mobs.griffin_warn" -> dianaMobsGriffinWarn;
            case "qol.diana_profit.hud" -> dianaProfitHud;
            case "qol.diana_share.party" -> dianaShareParty;
            case "qol.diana_share.auto_warp" -> dianaShareAutoWarp;
            case "qol.foraging_trees.progress_hud" -> foragingTreesProgressHud;
            case "qol.foraging_trees.only_axe" -> foragingTreesOnlyAxe;
            case "qol.foraging_trees.hide_bits" -> foragingTreesHideBits;
            case "qol.foraging_trees.gift_hud" -> foragingTreesGiftHud;
            case "qol.foraging_trees.hide_unmineable" -> foragingTreesHideUnmineable;
            case "qol.foraging_trees.fell_title" -> foragingTreesFellTitle;
            case "qol.foraging_audio.mute_phantom" -> foragingAudioMutePhantom;
            case "qol.foraging_audio.mute_tree_break" -> foragingAudioMuteTreeBreak;
            case "qol.foraging_audio.mute_break_galatea" -> foragingAudioMuteBreakGalatea;
            case "qol.foraging_audio.mute_fusion" -> foragingAudioMuteFusion;
            case "qol.foraging_audio.mute_stereo" -> foragingAudioMuteStereo;
            case "qol.foraging_helpers.sweep_hud" -> foragingHelpersSweepHud;
            case "qol.foraging_helpers.temple_solver" -> foragingHelpersTempleSolver;
            case "qol.foraging_helpers.beacon_hints" -> foragingHelpersBeaconHints;
            case "qol.foraging_helpers.highlights" -> foragingHelpersHighlights;
            case "qol.foraging_helpers.moonglade_beacon" -> foragingHelpersMoongladeBeacon;
            case "qol.foraging_helpers.park_tutorial" -> foragingHelpersParkTutorial;
            case "qol.foraging_helpers.hunting_esp" -> foragingHelpersHuntingEsp;
            case "qol.foraging_helpers.frog_mask" -> foragingHelpersFrogMask;
            case "qol.foraging_helpers.lasso_hud" -> foragingHelpersLassoHud;
            case "qol.foraging_helpers.cinderbat" -> foragingHelpersCinderbat;
            case "qol.foraging_helpers.huntaxe_lock" -> foragingHelpersHuntaxeLock;
            case "qol.foraging_helpers.shard_tracker" -> foragingHelpersShardTracker;
            case "qol.foraging_helpers.lasso_alert" -> foragingHelpersLassoAlert;
            case "qol.foraging_helpers.hotf_hint" -> foragingHelpersHotfHint;
            case "qol.foraging_cheats.auto_beacon" -> foragingCheatsAutoBeacon;
            case "qol.foraging_cheats.auto_chop" -> foragingCheatsAutoChop;
            case "qol.foraging_cheats.axe_toss" -> foragingCheatsAxeToss;
            case "qol.dungeon_hud.secrets" -> dungeonHudSecrets;
            case "qol.dungeon_hud.score" -> dungeonHudScore;
            case "qol.dungeon_hud.class" -> dungeonHudClass;
            case "qol.dungeon_hud.floor" -> dungeonHudFloor;
            case "qol.dungeon_hud.cleared" -> dungeonHudCleared;
            case "qol.dungeon_hud.invincibility" -> dungeonHudInvincibility;
            case "qol.dungeon_hud.mask_overlay" -> dungeonHudMaskOverlay;
            case "qol.dungeon_hud.terracotta" -> dungeonHudTerracotta;
            case "qol.dungeon_hud.blessings" -> dungeonHudBlessings;
            case "qol.dungeon_hud.f7_timers" -> dungeonHudF7Timers;
            case "qol.dungeon_hud.ragnarock" -> dungeonHudRagnarock;
            case "qol.dungeon_hud.melody" -> dungeonHudMelody;
            case "qol.dungeon_hud.melody_other" -> dungeonHudMelodyOther;
            case "qol.dungeon_hud.quiz" -> dungeonHudQuiz;
            case "qol.dungeon_hud.map" -> dungeonHudMap;
            case "qol.dungeon_hud.map_doors" -> dungeonHudMapDoors;
            case "qol.dungeon_hud.map_players" -> dungeonHudMapPlayers;
            case "qol.dungeon_hud.map_extra" -> dungeonHudMapExtra;
            case "qol.dungeon_hud.map_hide_boss" -> dungeonHudMapHideBoss;
            case "qol.dungeon_hud.crypts" -> dungeonHudCrypts;
            case "qol.dungeon_hud.deaths" -> dungeonHudDeaths;
            case "qol.dungeon_hud.score_overlay" -> dungeonHudScoreOverlay;
            case "qol.dungeon_hud.class_icons" -> dungeonHudClassIcons;
            case "qol.dungeon_hud.head_markers" -> dungeonHudHeadMarkers;
            case "qol.dungeon_hud.room_names" -> dungeonHudRoomNames;
            case "qol.dungeon_hud.room_secrets" -> dungeonHudRoomSecrets;
            case "qol.dungeon_hud.player_names" -> dungeonHudPlayerNames;
            case "qol.dungeon_hud.map_mimic" -> dungeonHudMapMimic;
            case "qol.dungeon_hud.map_puzzles" -> dungeonHudMapPuzzles;
            case "qol.dungeon_hud.puzzle_timer" -> dungeonHudPuzzleTimer;
            case "qol.dungeon_hud.warp_cooldown" -> dungeonHudWarpCooldown;
            case "qol.dungeon_hud.secret_spawn" -> dungeonHudSecretSpawn;
            case "qol.dungeon_hud.explosive_shot" -> dungeonHudExplosiveShot;
            case "qol.dungeon_hud.unclaimed_chests" -> dungeonHudUnclaimedChests;
            case "qol.dungeon_hud.chest_warning" -> dungeonHudChestWarning;
            case "qol.dungeon_hud.extra_stats" -> dungeonHudExtraStats;
            case "qol.dungeon_hud.ledge" -> dungeonHudLedge;
            case "qol.dungeon_hud.ledge_yellow" -> dungeonHudLedgeYellow;
            case "qol.dungeon_hud.ledge_all" -> dungeonHudLedgeAll;
            case "qol.dungeon_hud.run_timers" -> dungeonHudRunTimers;
            case "qol.dungeon_hud.show_split_pbs" -> dungeonHudShowSplitPbs;
            case "qol.dungeon_hud.kuudra_splits" -> dungeonHudKuudraSplits;
            case "qol.dungeon_hud.cheater_map" -> dungeonHudCheaterMap;
            case "qol.dungeon_hud.cheater_names" -> dungeonHudCheaterNames;
            case "qol.dungeon_hud.cheater_darken" -> dungeonHudCheaterDarken;
            case "qol.dungeon_esp.starred" -> dungeonEspStarred;
            case "qol.dungeon_esp.teammates" -> dungeonEspTeammates;
            case "qol.dungeon_esp.bats" -> dungeonEspBats;
            case "qol.dungeon_esp.fels" -> dungeonEspFels;
            case "qol.dungeon_esp.shadow" -> dungeonEspShadow;
            case "qol.dungeon_esp.keys" -> dungeonEspKeys;
            case "qol.dungeon_esp.mimic" -> dungeonEspMimic;
            case "qol.dungeon_esp.wither" -> dungeonEspWither;
            case "qol.dungeon_esp.crystals" -> dungeonEspCrystals;
            case "qol.dungeon_esp.secrets" -> dungeonEspSecrets;
            case "qol.dungeon_esp.secret_waypoints" -> dungeonEspSecretWaypoints;
            case "qol.dungeon_esp.hide_collected" -> dungeonEspHideCollected;
            case "qol.dungeon_esp.secret_clicked" -> dungeonEspSecretClicked;
            case "qol.dungeon_esp.secret_clicked_boss" -> dungeonEspSecretClickedBoss;
            case "qol.dungeon_esp.items" -> dungeonEspItems;
            case "qol.dungeon_esp.iced_mobs" -> dungeonEspIcedMobs;
            case "qol.dungeon_esp.simon" -> dungeonEspSimon;
            case "qol.dungeon_esp.hate_doors" -> dungeonEspHateDoors;
            case "qol.dungeon_esp.hate_wither" -> dungeonEspHateWither;
            case "qol.dungeon_esp.hate_blood" -> dungeonEspHateBlood;
            case "qol.dungeon_esp.hate_entrance" -> dungeonEspHateEntrance;
            case "qol.dungeon_esp.livid" -> dungeonEspLivid;
            case "qol.dungeon_esp.thorn" -> dungeonEspThorn;
            case "qol.dungeon_esp.spirit_bear" -> dungeonEspSpiritBear;
            case "qol.dungeon_esp.doors" -> dungeonEspDoors;
            case "qol.dungeon_esp.blood_box" -> dungeonEspBloodBox;
            case "qol.dungeon_esp.tracers" -> dungeonEspTracers;
            case "qol.dungeon_esp.depth" -> dungeonEspDepth;
            case "qol.dungeon_esp.ghost_block" -> dungeonEspGhostBlock;
            case "qol.dungeon_esp.ghost_uayor" -> dungeonEspGhostUayor;
            case "qol.dungeon_esp.ghost_stonk" -> dungeonEspGhostStonk;
            case "qol.dungeon_esp.triggerbot" -> dungeonEspTriggerBot;
            case "qol.dungeon_esp.trigger_crystal" -> dungeonEspTriggerCrystal;
            case "qol.dungeon_esp.trigger_take" -> dungeonEspTriggerTake;
            case "qol.dungeon_esp.trigger_place" -> dungeonEspTriggerPlace;
            case "qol.dungeon_esp.trigger_secret" -> dungeonEspTriggerSecret;
            case "qol.dungeon_esp.fill" -> dungeonEspFill;
            case "qol.dungeon_announce.mimic" -> dungeonAnnounceMimic;
            case "qol.dungeon_announce.prince" -> dungeonAnnouncePrince;
            case "qol.dungeon_announce.bat" -> dungeonAnnounceBat;
            case "qol.dungeon_announce.blood" -> dungeonAnnounceBlood;
            case "qol.dungeon_announce.score_title" -> dungeonAnnounceScoreTitle;
            case "qol.dungeon_announce.f7" -> dungeonAnnounceF7;
            case "qol.dungeon_announce.ragnarock" -> dungeonAnnounceRagnarock;
            case "qol.dungeon_announce.rooms" -> dungeonAnnounceRooms;
            case "qol.dungeon_announce.melody" -> dungeonAnnounceMelody;
            case "qol.dungeon_announce.melody_party" -> dungeonAnnounceMelodyParty;
            case "qol.dungeon_announce.melody_progress" -> dungeonAnnounceMelodyProgress;
            case "qol.dungeon_announce.death" -> dungeonAnnounceDeath;
            case "qol.dungeon_announce.position" -> dungeonAnnouncePosition;
            case "qol.dungeon_announce.secret_chime" -> dungeonAnnounceSecretChime;
            case "qol.dungeon_announce.duplicate_class" -> dungeonAnnounceDuplicateClass;
            case "qol.dungeon_announce.player_count" -> dungeonAnnouncePlayerCount;
            case "qol.dungeon_announce.location" -> dungeonAnnounceLocation;
            case "qol.dungeon_announce.key_drop" -> dungeonAnnounceKeyDrop;
            case "qol.dungeon_announce.key_drop_all" -> dungeonAnnounceKeyDropAll;
            case "qol.dungeon_announce.auto_ult" -> dungeonAnnounceAutoUlt;
            case "qol.dungeon_leap.highlight" -> dungeonLeapHighlight;
            case "qol.dungeon_leap.custom_gui" -> dungeonLeapCustomGui;
            case "qol.dungeon_leap.announce" -> dungeonLeapAnnounce;
            case "qol.dungeon_leap.counter" -> dungeonLeapCounter;
            case "qol.dungeon_terminals.overlay" -> dungeonTerminalsOverlay;
            case "qol.dungeon_terminals.auto" -> dungeonTerminalsAuto;
            case "qol.dungeon_terminals.melody" -> dungeonTerminalsMelody;
            case "qol.dungeon_terminals.numbers" -> dungeonTerminalsNumbers;
            case "qol.dungeon_terminals.numbers_show" -> dungeonTerminalsNumbersShow;
            case "qol.dungeon_terminals.rubix" -> dungeonTerminalsRubix;
            case "qol.dungeon_terminals.colors" -> dungeonTerminalsColors;
            case "qol.dungeon_terminals.panes" -> dungeonTerminalsPanes;
            case "qol.dungeon_terminals.starts" -> dungeonTerminalsStarts;
            case "qol.dungeon_terminals.auto_melody" -> dungeonTerminalsAutoMelody;
            case "qol.dungeon_terminals.melody_skip" -> dungeonTerminalsMelodySkip;
            case "qol.dungeon_terminals.melody_skip_first_row" -> dungeonTerminalsMelodySkipFirstRow;
            case "qol.dungeon_terminals.sounds" -> dungeonTerminalsSounds;
            case "qol.dungeon_terminals.complete_sounds" -> dungeonTerminalsCompleteSounds;
            case "qol.dungeon_terminals.stop_tooltips" -> dungeonTerminalsStopTooltips;
            case "qol.dungeon_terminals.hide_clicked" -> dungeonTerminalsHideClicked;
            case "qol.dungeon_terminals.block_wrong_slots" -> dungeonTerminalsBlockWrongSlots;
            case "qol.dungeon_terminals.human_order" -> dungeonTerminalsHumanOrder;
            case "qol.dungeon_terminals.melody_keys" -> dungeonTerminalsMelodyKeys;
            case "qol.dungeon_terminals.protect" -> dungeonTerminalsProtect;
            case "qol.dungeon_terminals.auto_numbers" -> dungeonTerminalsAutoNumbers;
            case "qol.dungeon_terminals.auto_colors" -> dungeonTerminalsAutoColors;
            case "qol.dungeon_terminals.auto_rubix" -> dungeonTerminalsAutoRubix;
            case "qol.dungeon_terminals.auto_panes" -> dungeonTerminalsAutoPanes;
            case "qol.dungeon_terminals.auto_starts" -> dungeonTerminalsAutoStarts;
            case "qol.dungeon_terminals.queue" -> dungeonTerminalsQueue;
            case "qol.dungeon_terminals.clone" -> dungeonTerminalsClone;
            case "qol.dungeon_terminals.hitboxes" -> dungeonTerminalsHitboxes;
            case "qol.dungeon_puzzles.quiz" -> dungeonPuzzlesQuiz;
            case "qol.dungeon_puzzles.quiz_boxes" -> dungeonPuzzlesQuizBoxes;
            case "qol.dungeon_puzzles.quiz_timer" -> dungeonPuzzlesQuizTimer;
            case "qol.dungeon_puzzles.weirdos" -> dungeonPuzzlesWeirdos;
            case "qol.dungeon_puzzles.blaze" -> dungeonPuzzlesBlaze;
            case "qol.dungeon_puzzles.ice" -> dungeonPuzzlesIce;
            case "qol.dungeon_puzzles.ice_path" -> dungeonPuzzlesIcePath;
            case "qol.dungeon_puzzles.ice_optimize" -> dungeonPuzzlesIceOptimize;
            case "qol.dungeon_puzzles.water" -> dungeonPuzzlesWater;
            case "qol.dungeon_puzzles.water_optimized" -> dungeonPuzzlesWaterOptimized;
            case "qol.dungeon_puzzles.boulder" -> dungeonPuzzlesBoulder;
            case "qol.dungeon_puzzles.tp_maze" -> dungeonPuzzlesTpMaze;
            case "qol.dungeon_puzzles.creeper_beams" -> dungeonPuzzlesCreeperBeams;
            case "qol.dungeon_puzzles.tic_tac_toe" -> dungeonPuzzlesTicTacToe;
            case "qol.dungeon_f7.titles" -> dungeonF7Titles;
            case "qol.dungeon_f7.title_crystal" -> dungeonF7TitleCrystal;
            case "qol.dungeon_f7.title_wither" -> dungeonF7TitleWither;
            case "qol.dungeon_f7.title_terminal" -> dungeonF7TitleTerminal;
            case "qol.dungeon_f7.title_gate" -> dungeonF7TitleGate;
            case "qol.dungeon_f7.hide_other_titles" -> dungeonF7HideOtherTitles;
            case "qol.dungeon_f7.hide_titles_at_ss" -> dungeonF7HideTitlesAtSs;
            case "qol.dungeon_f7.hide_titles_at_pre4" -> dungeonF7HideTitlesAtPre4;
            case "qol.dungeon_f7.hide_at_ss" -> dungeonF7HideAtSs;
            case "qol.dungeon_f7.hide_at_ss_pre_terms" -> dungeonF7HideAtSsPreTerms;
            case "qol.dungeon_f7.hide_after_leap" -> dungeonF7HideAfterLeap;
            case "qol.dungeon_f7.hide_after_leap_boss" -> dungeonF7HideAfterLeapBoss;
            case "qol.dungeon_f7.timers" -> dungeonF7Timers;
            case "qol.dungeon_f7.timer_ticks" -> dungeonF7TimerTicks;
            case "qol.dungeon_f7.timer_symbol" -> dungeonF7TimerSymbol;
            case "qol.dungeon_f7.timer_prefix" -> dungeonF7TimerPrefix;
            case "qol.dungeon_f7.timer_maxor" -> dungeonF7TimerMaxor;
            case "qol.dungeon_f7.timer_storm" -> dungeonF7TimerStorm;
            case "qol.dungeon_f7.timer_pad" -> dungeonF7TimerPad;
            case "qol.dungeon_f7.timer_lightning" -> dungeonF7TimerLightning;
            case "qol.dungeon_f7.timer_goldor" -> dungeonF7TimerGoldor;
            case "qol.dungeon_f7.timer_necron" -> dungeonF7TimerNecron;
            case "qol.dungeon_f7.crystals" -> dungeonF7Crystals;
            case "qol.dungeon_f7.maxor_stun" -> dungeonF7MaxorStun;
            case "qol.dungeon_f7.storm_crush" -> dungeonF7StormCrush;
            case "qol.dungeon_f7.storm_lb" -> dungeonF7StormLb;
            case "qol.dungeon_f7.term_start" -> dungeonF7TermStart;
            case "qol.dungeon_f7.crystal_spawn" -> dungeonF7CrystalSpawn;
            case "qol.dungeon_f7.crystal_place" -> dungeonF7CrystalPlace;
            case "qol.dungeon_f7.crystal_alert" -> dungeonF7CrystalAlert;
            case "qol.dungeon_f7.melody_display" -> dungeonF7MelodyDisplay;
            case "qol.dungeon_f7.wither_esp" -> dungeonF7WitherEsp;
            case "qol.dungeon_f7.dragons" -> dungeonF7Dragons;
            case "qol.dungeon_f7.dragon_spray" -> dungeonF7DragonSpray;
            case "qol.dungeon_f7.dragon_arrows" -> dungeonF7DragonArrows;
            case "qol.dungeon_f7.dragon_health" -> dungeonF7DragonHealth;
            case "qol.dungeon_f7.dragon_boxes" -> dungeonF7DragonBoxes;
            case "qol.dungeon_f7.dragon_tracers" -> dungeonF7DragonTracers;
            case "qol.dungeon_f7.dragon_priority" -> dungeonF7DragonPriority;
            case "qol.dungeon_f7.dragon_paul" -> dungeonF7DragonPaul;
            case "qol.dungeon_f7.p3_display" -> dungeonF7P3Display;
            case "qol.dungeon_f7.sharp_shooter" -> dungeonF7SharpShooter;
            case "qol.dungeon_f7.sharp_aim" -> dungeonF7SharpAim;
            case "qol.dungeon_f7.sharp_complete" -> dungeonF7SharpComplete;
            case "qol.dungeon_f7.term_times" -> dungeonF7TermTimes;
            case "qol.dungeon_f7.term_pbs" -> dungeonF7TermPbs;
            case "qol.dungeon_f7.predev" -> dungeonF7Predev;
            case "qol.dungeon_f7.predev_all" -> dungeonF7PredevAll;
            case "qol.dungeon_f7.ss_complete" -> dungeonF7SsComplete;
            case "qol.dungeon_f7.pre4_complete" -> dungeonF7Pre4Complete;
            case "qol.dungeon_f7.dragon_timer" -> dungeonF7DragonTimer;
            case "qol.dungeon_f7.goldor_frenzy" -> dungeonF7GoldorFrenzy;
            case "qol.dungeon_f7.purple_pad" -> dungeonF7PurplePad;
            case "qol.dungeon_leap.keys" -> dungeonLeapKeys;
            case "qol.dungeon_f7.simon" -> dungeonF7Simon;
            case "qol.dungeon_f7.simon_progress" -> dungeonF7SimonProgress;
            case "qol.dungeon_f7.simon_block_wrong" -> dungeonF7SimonBlockWrong;
            case "qol.dungeon_f7.simon_auto" -> dungeonF7SimonAuto;
            case "qol.dungeon_f7.simon_trigger" -> dungeonF7SimonTrigger;
            case "qol.dungeon_f7.simon_sounds" -> dungeonF7SimonSounds;
            case "qol.dungeon_f7.hide_diorite" -> dungeonF7HideDiorite;
            case "qol.dungeon_f7.arrow_align" -> dungeonF7ArrowAlign;
            case "qol.dungeon_f7.arrow_block_wrong" -> dungeonF7ArrowBlockWrong;
            case "qol.dungeon_f7.i4" -> dungeonF7I4;
            case "qol.dungeon_f7.i4_predict" -> dungeonF7I4Predict;
            case "qol.dungeon_f7.auto_i4" -> dungeonF7AutoI4;
            case "qol.dungeon_f7.auto_i4_rod" -> dungeonF7AutoI4Rod;
            case "qol.dungeon_f7.auto_i4_mask" -> dungeonF7AutoI4Mask;
            case "qol.dungeon_f7.auto_i4_leap" -> dungeonF7AutoI4Leap;
            case "qol.dungeon_f7.auto_i4_leap_melody" -> dungeonF7AutoI4LeapMelody;
            case "qol.dungeon_f7.debuff" -> dungeonF7Debuff;
            case "qol.dungeon_f7.debuff_auto" -> dungeonF7DebuffAuto;
            case "qol.dungeon_f7.debuff_ice" -> dungeonF7DebuffIce;
            case "qol.dungeon_f7.debuff_gravity" -> dungeonF7DebuffGravity;
            case "qol.dungeon_f7.gate" -> dungeonF7Gate;
            case "qol.dungeon_f7.relics" -> dungeonF7Relics;
            case "qol.dungeon_f7.relic_look" -> dungeonF7RelicLook;
            case "qol.dungeon_f7.relic_spawn" -> dungeonF7RelicSpawn;
            case "qol.dungeon_f7.relic_beacon" -> dungeonF7RelicBeacon;
            case "qol.dungeon_f7.relic_place" -> dungeonF7RelicPlace;
            case "qol.dungeon_f7.relic_highlight" -> dungeonF7RelicHighlight;
            case "qol.dungeon_f7.relic_block_wrong" -> dungeonF7RelicBlockWrong;
            case "qol.dungeon_f7.breaker_prevent_secrets" -> dungeonF7BreakerPreventSecrets;
            case "qol.dungeon_f7.breaker_charges" -> dungeonF7BreakerCharges;
            case "qol.dungeon_f7.auto_superboom" -> dungeonF7AutoSuperboom;
            case "qol.dungeon_f7.superboom_swap_back" -> dungeonF7SuperboomSwapBack;
            case "qol.dungeon_menus.salvage" -> dungeonMenusSalvage;
            case "qol.dungeon_menus.party_finder" -> dungeonMenusPartyFinder;
            case "qol.dungeon_menus.chest_profit" -> dungeonMenusChestProfit;
            case "qol.dungeon_menus.chest_spin" -> dungeonMenusChestSpin;
            case "qol.dungeon_menus.include_essence" -> dungeonMenusIncludeEssence;
            case "qol.dungeon_menus.include_cost" -> dungeonMenusIncludeCost;
            case "qol.dungeon_menus.compact_profit" -> dungeonMenusCompactProfit;
            case "qol.dungeon_menus.close_chest" -> dungeonMenusCloseChest;
            case "qol.farm_keys.lock_camera" -> farmKeysLockCamera;
            case "qol.dungeon_termsim.show_pbs" -> dungeonTermSimShowPbs;
            case "qol.camera.clip" -> cameraClip;
            case "qol.camera.custom_distance" -> cameraCustomDistance;
            case "qol.slayer_display.kill_time" -> slayerDisplayKillTime;
            case "qol.slayer_display.dynamic_size" -> slayerDisplayDynamicSize;
            case "qol.slayer_time_messages.time_to_kill" -> slayerTimeMessagesTimeToKill;
            case "qol.slayer_time_messages.personal_best" -> slayerTimeMessagesPersonalBest;
            case "qol.slayer_time_messages.quest_complete" -> slayerTimeMessagesQuestComplete;
            case "qol.slayer_time_messages.compact" -> slayerTimeMessagesCompact;
            case "qol.slayer_progress.show_remaining" -> slayerProgressShowRemaining;
            case "qol.slayer_progress.boss_warning" -> slayerProgressBossWarning;
            case "qol.slayer_progress.warning_repeat" -> slayerProgressWarningRepeat;
            case "qol.slayer_stats.bosses_killed" -> slayerStatsBossesKilled;
            case "qol.slayer_stats.bosses_per_hour" -> slayerStatsBossesPerHour;
            case "qol.slayer_stats.average_kill_time" -> slayerStatsAverageKillTime;
            case "qol.slayer_stats.session_time" -> slayerStatsSessionTime;
            case "qol.slayer_highlights.only_mine" -> slayerHighlightsOnlyMine;
            case "qol.slayer_highlights.boss" -> slayerHighlightsBoss;
            case "qol.slayer_highlights.miniboss" -> slayerHighlightsMiniboss;
            case "qol.slayer_highlights.demon" -> slayerHighlightsDemon;
            case "qol.slayer_highlights.depth" -> slayerHighlightsDepth;
            case "qol.slayer_highlights.target_lines" -> slayerHighlightsTargetLines;
            case "qol.slayer_highlights.hide_spawn_particles" -> slayerHighlightsHideSpawnParticles;
            case "qol.slayer_highlights.hide_damage_splash" -> slayerHighlightsHideDamageSplash;
            case "qol.slayer_highlights.hide_mob_names" -> slayerHighlightsHideMobNames;
            case "qol.slayer_active_boss_transparency.other_players" ->
                    slayerActiveBossTransparencyPlayers;
            case "qol.slayer_miniboss_alert.message" -> slayerMinibossAlertMessage;
            case "qol.slayer_miniboss_alert.title" -> slayerMinibossAlertTitle;
            case "qol.slayer_drops.show_chance" -> slayerDropsShowChance;
            case "qol.slayer_drops.bosses_since" -> slayerDropsBossesSince;
            case "qol.slayer_drops.detect_automatically" -> slayerDropsDetectAutomatically;
            case "qol.slayer_drops.rng_hud" -> slayerDropsRngHud;
            case "qol.slayer_drops.rng_warn_empty" -> slayerDropsRngWarnEmpty;
            case "qol.slayer_drops.rng_hide_chat" -> slayerDropsRngHideChat;
            case "qol.slayer_drops.profit_hud" -> slayerDropsProfitHud;
            case "qol.slayer_drops.profit_table" -> slayerDropsProfitTable;
            case "qol.slayer_drops.profit_per_hour" -> slayerDropsProfitPerHour;
            case "qol.slayer_drops.profit_hide_outside_inventory" -> slayerDropsProfitHideOutsideInventory;
            case "qol.slayer_drops.ground_highlight" -> slayerDropsGroundHighlight;
            case "qol.slayer_drops.ground_labels" -> slayerDropsGroundLabels;
            case "qol.slayer_drops.price_in_chat" -> slayerDropsPriceInChat;
            case "qol.slayer_drops.price_title" -> slayerDropsPriceTitle;
            case "qol.slayer_drops.price_title_sound" -> slayerDropsPriceTitleSound;
            case "qol.slayer_drops.recent_highlight" -> slayerDropsRecentHighlight;
            case "qol.slayer_carry.announce_party" -> slayerCarryAnnounceParty;
            case "qol.slayer_carry.show_spawn_message" -> slayerCarryShowSpawnMessage;
            case "qol.slayer_carry.display" -> slayerCarryDisplay;
            case "qol.slayer_carry.webhook" -> slayerCarryWebhook;
            case "qol.slayer_carry.webhook_each" -> slayerCarryWebhookEach;
            case "qol.slayer_cocoon_alert.show_alert" -> slayerCocoonShowAlert;
            case "qol.slayer_cocoon_alert.timer" -> slayerCocoonTimer;
            case "qol.slayer_laser_hider.show_for_carries" -> slayerLaserShowForCarries;
            case "qol.slayer_attunement_display.count" -> slayerAttunementDisplayCount;
            case "qol.slayer_auto_soulcry.check_mana" -> slayerAutoSoulcryCheckMana;
            case "qol.slayer_auto_soulcry.check_hitbox" -> slayerAutoSoulcryCheckHitbox;
            case "qol.slayer_auto_soulcry.tick_based" -> slayerAutoSoulcryTickBased;
            case "qol.slayer_auto_soulcry.attack_based" -> slayerAutoSoulcryAttackBased;
            case "qol.slayer_auto_soulcry.other_bosses" -> slayerAutoSoulcryOtherBosses;
            case "qol.slayer_sounds.disable_voidgloom" -> slayerSoundsDisableVoidgloom;
            case "qol.slayer_sounds.disable_vampire" -> slayerSoundsDisableVampire;
            case "qol.slayer_voidgloom.highlight_beacon" -> slayerVoidgloomHighlightBeacon;
            case "qol.slayer_voidgloom.beacon_warning" -> slayerVoidgloomBeaconWarning;
            case "qol.slayer_voidgloom.beacon_sound" -> slayerVoidgloomBeaconSound;
            case "qol.slayer_voidgloom.beacon_line" -> slayerVoidgloomBeaconLine;
            case "qol.slayer_voidgloom.beacon_path" -> slayerVoidgloomBeaconPath;
            case "qol.slayer_voidgloom.beacon_timer" -> slayerVoidgloomBeaconTimer;
            case "qol.slayer_voidgloom.highlight_held" -> slayerVoidgloomHighlightHeld;
            case "qol.slayer_voidgloom.highlight_nukekubi" -> slayerVoidgloomHighlightNukekubi;
            case "qol.slayer_voidgloom.nukekubi_line" -> slayerVoidgloomNukekubiLine;
            case "qol.slayer_voidgloom.world_labels" -> slayerVoidgloomWorldLabels;
            case "qol.slayer_voidgloom.line_to_boss" -> slayerVoidgloomLineToBoss;
            case "qol.slayer_voidgloom.phase_display" -> slayerVoidgloomPhaseDisplay;
            case "qol.slayer_voidgloom.hits_display" -> slayerVoidgloomHitsDisplay;
            case "qol.slayer_voidgloom.laser_timer" -> slayerVoidgloomLaserTimer;
            case "qol.slayer_voidgloom.laser_health" -> slayerVoidgloomLaserHealth;
            case "qol.slayer_voidgloom.hide_particles" -> slayerVoidgloomHideParticles;
            case "qol.slayer_revenant.boom_display" -> slayerRevenantBoomDisplay;
            case "qol.slayer_revenant.boom_sound" -> slayerRevenantBoomSound;
            case "qol.slayer_revenant.boom_highlight" -> slayerRevenantBoomHighlight;
            case "qol.slayer_revenant.world_labels" -> slayerRevenantWorldLabels;
            case "qol.slayer_revenant.line_to_boss" -> slayerRevenantLineToBoss;
            case "qol.slayer_tarantula.highlight_egg_sacs" -> slayerTarantulaHighlightEggSacs;
            case "qol.slayer_tarantula.egg_hits" -> slayerTarantulaEggHits;
            case "qol.slayer_tarantula.highlight_invincible" -> slayerTarantulaHighlightInvincible;
            case "qol.slayer_tarantula.invincible_text" -> slayerTarantulaInvincibleText;
            case "qol.slayer_tarantula.phase_display" -> slayerTarantulaPhaseDisplay;
            case "qol.slayer_tarantula.world_labels" -> slayerTarantulaWorldLabels;
            case "qol.slayer_tarantula.mute_sounds" -> slayerTarantulaMuteSounds;
            case "qol.slayer_tarantula.line_to_boss" -> slayerTarantulaLineToBoss;
            case "qol.slayer_sven.highlight_pups" -> slayerSvenHighlightPups;
            case "qol.slayer_sven.pup_line" -> slayerSvenPupLine;
            case "qol.slayer_sven.world_labels" -> slayerSvenWorldLabels;
            case "qol.slayer_sven.hide_pup_nametags" -> slayerSvenHidePupNametags;
            case "qol.slayer_sven.howl_warning" -> slayerSvenHowlWarning;
            case "qol.slayer_sven.mute_sounds" -> slayerSvenMuteSounds;
            case "qol.slayer_sven.line_to_boss" -> slayerSvenLineToBoss;
            case "qol.slayer_vampire_markers.blood_ichor" -> slayerVampireMarkersBloodIchor;
            case "qol.slayer_vampire_markers.killer_spring" -> slayerVampireMarkersKillerSpring;
            case "qol.slayer_vampire_markers.twinclaws" -> slayerVampireMarkersTwinclaws;
            case "qol.slayer_vampire_markers.mania" -> slayerVampireMarkersMania;
            case "qol.slayer_vampire_markers.mania_timer" -> slayerVampireMarkersManiaTimer;
            case "qol.slayer_vampire_markers.steak_alert" -> slayerVampireMarkersSteakAlert;
            case "qol.slayer_vampire_markers.world_labels" -> slayerVampireMarkersWorldLabels;
            case "qol.slayer_vampire_markers.mute_sounds" -> slayerVampireMarkersMuteSounds;
            case "qol.slayer_vampire_markers.line_to_boss" -> slayerVampireMarkersLineToBoss;
            case "qol.slayer_vampire_markers.ichor_beam" -> slayerVampireMarkersIchorBeam;
            case "qol.slayer_vampire_markers.chalice" -> slayerVampireMarkersChalice;
            case "qol.slayer_vampire_markers.effigies" -> slayerVampireMarkersEffigies;
            case "qol.slayer_inferno.fire_pillar" -> slayerInfernoFirePillar;
            case "qol.slayer_inferno.fire_pillar_sound" -> slayerInfernoFirePillarSound;
            case "qol.slayer_inferno.fire_pits" -> slayerInfernoFirePits;
            case "qol.slayer_inferno.phase_display" -> slayerInfernoPhaseDisplay;
            case "qol.slayer_inferno.color_by_attunement" -> slayerInfernoColorByAttunement;
            case "qol.slayer_inferno.hide_chat" -> slayerInfernoHideChat;
            case "qol.slayer_inferno.hide_particles" -> slayerInfernoHideParticles;
            case "qol.slayer_inferno.world_labels" -> slayerInfernoWorldLabels;
            case "qol.slayer_inferno.line_to_boss" -> slayerInfernoLineToBoss;
            case "qol.slayer_inferno.gummy_warning" -> slayerInfernoGummyWarning;
            case "qol.slayer_quest_warning.title" -> slayerQuestWarningTitle;
            case "qol.slayer_quest_warning.chat" -> slayerQuestWarningChat;
            case "qol.slayer_auto_start.block_not_spawnable" -> slayerAutoStartBlockNotSpawnable;
            case "qol.slayer_vengeance.compact" -> slayerVengeanceCompact;
            case "qol.slayer_vengeance.use_ticks" -> slayerVengeanceUseTicks;
            case "qol.slayer_vengeance_damage.abbreviate" -> slayerVengeanceDamageAbbreviate;
            case "qol.slayer_big_drops.revenant" -> slayerBigDropsRevenant;
            case "qol.slayer_big_drops.tarantula" -> slayerBigDropsTarantula;
            case "qol.slayer_big_drops.sven" -> slayerBigDropsSven;
            case "qol.slayer_big_drops.voidgloom" -> slayerBigDropsVoidgloom;
            case "qol.slayer_big_drops.inferno" -> slayerBigDropsInferno;
            case "qol.slayer_big_drops.vampire" -> slayerBigDropsVampire;
            case "qol.storage_overlay.always_open" -> storageOverlayAlwaysOpen;
            case "qol.storage_overlay.outline_active" -> storageOverlayOutlineActive;
            case "qol.storage_overlay.inactive_tooltips" -> storageOverlayInactiveTooltips;
            case "qol.storage_overlay.retain_scroll" -> storageOverlayRetainScroll;
            case "qol.storage_overlay.invert_scroll" -> storageOverlayInvertScroll;
            case "qol.storage_overlay.block_item_scroll" -> storageOverlayBlockItemScroll;
            case "qol.storage_overlay.highlight_search" -> storageOverlayHighlightSearch;
            case "qol.storage_overlay.filter_search" -> storageOverlayFilterSearch;
            case "qol.storage_overlay.item_search" -> storageItemSearch;
            case "qol.storage_overlay.craft_helper" -> storageCraftHelper;
            case "qol.storage_overlay.museum_armor" -> storageMuseumArmor;
            case "qol.inventory_buttons.hover_tooltip" -> inventoryButtonsHoverTooltip;
            case "qol.inventory_buttons.inventory_only" -> inventoryButtonsInventoryOnly;
            case "qol.reward_claim.hide_chat_link" -> rewardClaimMuteChatLink;
            case "qol.reward_claim.block_browser" -> rewardClaimBlockBrowser;
            case "qol.reward_claim.wait_for_ad" -> rewardClaimWaitForAd;
            case "qol.freecam.show_body" -> freecamShowBody;
            case "qol.freecam.collide" -> freecamCollide;
            case "qol.hud_layout.show_background" -> hudLayoutShowBackground;
            case "qol.pet_hud.show_background" -> resolvedHudStyle("pet").showBackground;
            case "qol.performance_hud.show_background" -> resolvedHudStyle("performance").showBackground;
            case "qol.hud_layout.dim_unfocused" -> hudLayoutDimUnfocused;
            case "qol.hud_layout.hide_hotbar" -> hudHideHotbar;
            case "qol.hud_layout.hide_health" -> hudHideHealth;
            case "qol.hud_layout.hide_food" -> hudHideFood;
            case "qol.hud_layout.hide_armor" -> hudHideArmor;
            case "qol.hud_layout.hide_xp" -> hudHideXp;
            case "qol.hud_layout.hide_air" -> hudHideAir;
            case "qol.hud_layout.hide_mount" -> hudHideMount;
            case "qol.hud_layout.hide_scoreboard" -> hudHideScoreboard;
            case "qol.hud_layout.hide_boss" -> hudHideBoss;
            case "qol.hud_layout.hide_action" -> hudHideAction;
            case "qol.hud_layout.hide_item_name" -> hudHideItemName;
            case "qol.hud_layout.hide_effects" -> hudHideEffects;
            case "qol.hud_layout.hide_titles" -> hudHideTitles;
            case "qol.hud_layout.hide_tab" -> hudHideTab;
            case "qol.custom_cursor.click_anim" -> customCursorClickAnim;
            case "qol.custom_cursor.hold_anim" -> customCursorHoldAnim;
            case "qol.custom_cursor.hide_vanilla" -> customCursorHideVanilla;
            case "qol.legacy_textures.items" -> legacyTexturesItems;
            case "qol.custom_resource_pack.overworld" -> customResourcePackOverworld;
            case "qol.custom_resource_pack.crimson" -> customResourcePackCrimson;
            case "qol.custom_resource_pack.end" -> customResourcePackEnd;
            case "qol.custom_resource_pack.gameplay_font" -> customResourcePackGameplayFont;
            case "qol.render_optimizer.full_text_shadow" -> fullTextShadow;
            case "qol.iota.party_join_sound" -> iotaPartyJoinSound;
            case "qol.iota.limbo_alert" -> iotaLimboAlert;
            case "qol.iota.fix_fishing_hook" -> iotaFixFishingHook;
            case "qol.iota.mute_fishing_cast" -> iotaMuteFishingCast;
            case "qol.iota.mute_terminator" -> iotaMuteTerminator;
            case "qol.iota.party_commands" -> iotaPartyCommands;
            case "qol.iota.party_warp" -> iotaPartyWarp;
            case "qol.iota.party_transfer" -> iotaPartyTransfer;
            case "qol.iota.party_ping" -> iotaPartyPing;
            case "qol.iota.party_allinvite" -> iotaPartyAllInvite;
            case "qol.iota.party_tps" -> iotaPartyTps;
            case "qol.iota.party_promote" -> iotaPartyPromote;
            case "qol.iota.party_kick" -> iotaPartyKick;
            case "qol.iota.party_kuudra" -> iotaPartyKuudra;
            case "qol.iota.party_chests" -> iotaPartyChests;
            case "qol.iota.party_runs" -> iotaPartyRuns;
            case "qol.iota.party_profit" -> iotaPartyProfit;
            case "qol.iota.arrow_tracker" -> iotaArrowTracker;
            case "qol.iota.arrow_notifications" -> iotaArrowNotifications;
            case "qol.iota.auto_requeue" -> iotaAutoRequeue;
            case "qol.iota.supply_waypoints" -> iotaSupplyWaypoints;
            case "qol.iota.supply_hitbox" -> iotaSupplyHitbox;
            case "qol.iota.supply_pull_circle" -> iotaSupplyPullCircle;
            case "qol.iota.supply_giant_hitbox" -> iotaSupplyGiantHitbox;
            case "qol.iota.pile_waypoints" -> iotaPileWaypoints;
            case "qol.iota.pile_names" -> iotaPileNames;
            case "qol.iota.pearl_waypoints" -> iotaPearlWaypoints;
            case "qol.iota.build_waypoints" -> iotaBuildWaypoints;
            case "qol.iota.stun_waypoints" -> iotaStunWaypoints;
            case "qol.iota.ichor_pool" -> iotaIchorPool;
            case "qol.iota.kuudra_hitbox" -> iotaKuudraHitbox;
            case "qol.iota.etherwarp_helper" -> iotaEtherwarpHelper;
            case "qol.iota.fresh_tools" -> iotaFreshTools;
            case "qol.iota.fresh_announce" -> iotaFreshAnnounce;
            case "qol.iota.fresh_party" -> iotaFreshParty;
            case "qol.iota.build_info" -> iotaBuildInfo;
            case "qol.iota.kuudra_titles" -> iotaKuudraTitles;
            case "qol.stall_market.bazaar_search" -> stallBazaarSearch;
            case "qol.stall_market.sell_protection" -> stallSellProtection;
            case "qol.stall_market.angry_coop" -> stallAngryCoop;
            case "qol.stall_market.bin_overlay" -> stallBinOverlay;
            case "qol.stall_market.ah_highlight" -> stallAhHighlight;
            default -> athen().readBoolean(settingId);
        };
    }

    boolean writeBoolean(String settingId, boolean value) {
        if (settingId != null && settingId.startsWith("qol.custom_scoreboard.")
                && board().writeBoolean(settingId, value)) {
            return true;
        }
        switch (settingId) {
            case "qol.animation_fix.dyes" -> animationDyes = value;
            case "qol.animation_fix.skins" -> animationSkins = value;
            case "qol.info_tooltips.dungeon_quality" -> infoDungeonQuality = value;
            case "qol.info_tooltips.created_date" -> infoCreatedDate = value;
            case "qol.info_tooltips.hex_color" -> infoHexColor = value;
            case "qol.info_tooltips.museum" -> infoMuseum = value;
            case "qol.info_tooltips.item_id" -> infoItemId = value;
            case "qol.info_tooltips.star_count" -> infoStarCount = value;
            case "qol.info_tooltips.pet_candy" -> infoPetCandy = value;
            case "qol.info_tooltips.revert_master_stars" -> infoRevertMasterStars = value;
            case "qol.calendar_date.minister" -> calendarMinister = value;
            case "qol.price_tooltips.lowest_bin" -> priceLowestBin = value;
            case "qol.price_tooltips.bazaar" -> priceBazaar = value;
            case "qol.price_tooltips.npc" -> priceNpc = value;
            case "qol.price_tooltips.motes" -> priceMotes = value;
            case "qol.price_tooltips.price_paid" -> pricePaid = value;
            case "qol.viewmodel.no_haste" -> viewmodelNoHaste = value;
            case "qol.viewmodel.no_equip" -> viewmodelNoEquip = value;
            case "qol.viewmodel.no_bow_swing" -> viewmodelNoBowSwing = value;
            case "qol.viewmodel.apply_to_hand" -> viewmodelApplyToHand = value;
            case "qol.render_optimizer.hide_empty_tooltips" -> hideEmptyTooltips = value;
            case "qol.render_optimizer.hide_break_particles" -> hideBreakParticles = value;
            case "qol.render_optimizer.hide_boss_bar" -> hideBossBar = value;
            case "qol.render_optimizer.hide_armor_bar" -> hideArmorBar = value;
            case "qol.render_optimizer.hide_food_bar" -> hideFoodBar = value;
            case "qol.render_optimizer.hide_fog" -> hideFog = value;
            case "qol.render_optimizer.hide_effect_display" -> hideEffectDisplay = value;
            case "qol.render_optimizer.hide_recipe_book" -> hideRecipeBook = value;
            case "qol.render_optimizer.hide_selected_item_name" -> hideSelectedItemName = value;
            case "qol.render_optimizer.hide_dead_entities" -> hideDeadEntities = value;
            case "qol.render_optimizer.hide_dead_poof" -> hideDeadPoof = value;
            case "qol.render_optimizer.hide_implosion_particles" -> hideImplosionParticles = value;
            case "qol.render_optimizer.hide_entity_fire" -> hideEntityFire = value;
            case "qol.render_optimizer.hide_mage_beam" -> hideMageBeam = value;
            case "qol.render_optimizer.hide_ice_spray" -> hideIceSpray = value;
            case "qol.render_optimizer.hide_powder_coating" -> hidePowderCoating = value;
            case "qol.render_optimizer.hide_guided_sheep" -> hideGuidedSheep = value;
            case "qol.render_optimizer.hide_bone_plating" -> hideBonePlating = value;
            case "qol.render_optimizer.hide_tree_bits" -> hideTreeBits = value;
            case "qol.render_optimizer.hide_nausea" -> hideNausea = value;
            case "qol.render_optimizer.hide_stuck_arrows" -> hideStuckArrows = value;
            case "qol.render_optimizer.nether_fog" -> netherFog = value;
            case "qol.render_optimizer.totem_animation" -> totemAnimation = value;
            case "qol.render_optimizer.mob_icons" -> mobIcons = value;
            case "qol.experiment_solver.chronomatron" -> experimentChronomatron = value;
            case "qol.experiment_solver.ultrasequencer" -> experimentUltrasequencer = value;
            case "qol.experiment_solver.superpairs" -> experimentSuperpairs = value;
            case "qol.experiment_solver.block_wrong_clicks" -> experimentBlockWrongClicks = value;
            case "qol.experiment_solver.hide_tooltip" -> experimentHideTooltip = value;
            case "qol.experiment_solver.hide_wrong_chronomatron" -> experimentHideWrongChrono = value;
            case "qol.experiment_solver.hide_wrong_ultrasequencer" -> experimentHideWrongUltra = value;
            case "qol.experiment_solver.private_island_only" -> experimentPrivateIslandOnly = value;
            case "qol.auto_experiments.auto_close" -> autoExperimentsAutoClose = value;
            case "qol.auto_experiments.get_max_xp" -> autoExperimentsGetMaxXp = value;
            case "qol.cheater_wardrobe.move_equip" -> cheaterWardrobeMoveEquip = value;
            case "qol.cheater_wardrobe.stationary_only" -> cheaterWardrobeStationaryOnly = value;
            case "qol.cheater_wardrobe.reset_open" -> cheaterWardrobeResetOpen = value;
            case "qol.auto_gfs.in_skyblock" -> autoGfsInSkyblock = value;
            case "qol.auto_gfs.in_kuudra" -> autoGfsInKuudra = value;
            case "qol.auto_gfs.in_dungeon" -> autoGfsInDungeon = value;
            case "qol.auto_gfs.refill_on_dungeon_start" -> autoGfsRefillOnDungeonStart = value;
            case "qol.auto_gfs.refill_on_timer" -> autoGfsRefillOnTimer = value;
            case "qol.auto_gfs.refill_pearl" -> autoGfsRefillPearl = value;
            case "qol.auto_gfs.refill_jerry" -> autoGfsRefillJerry = value;
            case "qol.auto_gfs.refill_tnt" -> autoGfsRefillTnt = value;
            case "qol.auto_gfs.refill_leap" -> autoGfsRefillLeap = value;
            case "qol.auto_gfs.refill_twilight" -> autoGfsRefillTwilight = value;
            case "qol.auto_gfs.auto_get_draft" -> autoGfsAutoGetDraft = value;
            case "qol.ghosts.show_ghosts" -> ghostsShowGhosts = value;
            case "qol.auto_dojo.control" -> autoDojoControl = value;
            case "qol.auto_dojo.mastery" -> autoDojoMastery = value;
            case "qol.auto_dojo.discipline" -> autoDojoDiscipline = value;
            case "qol.auto_dojo.discipline_attack" -> autoDojoDisciplineAttack = value;
            case "qol.fishing_creatures.hud" -> fishingCreaturesHud = value;
            case "qol.fishing_creatures.cap_notify" -> fishingCreaturesCapNotify = value;
            case "qol.fishing_creatures.timer_notify" -> fishingCreaturesTimerNotify = value;
            case "qol.fishing_creatures.rare_announce" -> fishingCreaturesRareAnnounce = value;
            case "qol.fishing_creatures.rare_sound" -> fishingCreaturesRareSound = value;
            case "qol.fishing_creatures.rare_party" -> fishingCreaturesRareParty = value;
            case "qol.fishing_creatures.rare_esp" -> fishingCreaturesRareEsp = value;
            case "qol.fishing_creatures.shorten_chat" -> fishingCreaturesShortenChat = value;
            case "qol.fishing_creatures.hide_common" -> fishingCreaturesHideCommon = value;
            case "qol.fishing_creatures.auto_attack" -> fishingCreaturesAutoAttack = value;
            case "qol.fishing_creatures.thunder_sparks" -> fishingCreaturesThunderSparks = value;
            case "qol.fishing_hotspots.circle" -> fishingHotspotsCircle = value;
            case "qol.fishing_hotspots.hide_particles" -> fishingHotspotsHideParticles = value;
            case "qol.fishing_hotspots.radar" -> fishingHotspotsRadar = value;
            case "qol.fishing_hotspots.tracer" -> fishingHotspotsTracer = value;
            case "qol.fishing_hotspots.despawn" -> fishingHotspotsDespawn = value;
            case "qol.fishing_trophy.titles" -> fishingTrophyTitles = value;
            case "qol.fishing_trophy.filter_chat" -> fishingTrophyFilterChat = value;
            case "qol.fishing_trophy.golden_timer" -> fishingTrophyGoldenTimer = value;
            case "qol.fishing_trophy.geyser" -> fishingTrophyGeyser = value;
            case "qol.fishing_trophy.sponge" -> fishingTrophySponge = value;
            case "qol.fishing_trophy.fillet" -> fishingTrophyFillet = value;
            case "qol.fishing_visuals.hide_other_bobbers" -> fishingVisualsHideOtherBobbers = value;
            case "qol.fishing_visuals.chum_hider" -> fishingVisualsChumHider = value;
            case "qol.fishing_visuals.mute_banshee" -> fishingVisualsMuteBanshee = value;
            case "qol.fishing_visuals.mute_drake" -> fishingVisualsMuteDrake = value;
            case "qol.fishing_tools.bait_hud" -> fishingToolsBaitHud = value;
            case "qol.fishing_tools.no_bait_warn" -> fishingToolsNoBaitWarn = value;
            case "qol.fishing_tools.bait_change" -> fishingToolsBaitChange = value;
            case "qol.fishing_tools.thunder_notify" -> fishingToolsThunderNotify = value;
            case "qol.fishing_tools.totem_hud" -> fishingToolsTotemHud = value;
            case "qol.mining_scatha.titles" -> miningScathaTitles = value;
            case "qol.mining_scatha.sounds" -> miningScathaSounds = value;
            case "qol.mining_scatha.cooldown" -> miningScathaCooldown = value;
            case "qol.mining_scatha.pet_drop" -> miningScathaPetDrop = value;
            case "qol.mining_scatha.pet_rarity" -> miningScathaPetRarity = value;
            case "qol.mining_scatha.hud" -> miningScathaHud = value;
            case "qol.mining_scatha.party" -> miningScathaParty = value;
            case "qol.mining_events.hud" -> miningEventsHud = value;
            case "qol.mining_events.titles" -> miningEventsTitles = value;
            case "qol.mining_events.goblin_esp" -> miningEventsGoblinEsp = value;
            case "qol.mining_glacite.pity_hud" -> miningGlacitePityHud = value;
            case "qol.mining_glacite.corpse_hud" -> miningGlaciteCorpseHud = value;
            case "qol.mining_glacite.cold_overlay" -> miningGlaciteColdOverlay = value;
            case "qol.mining_glacite.party_share" -> miningGlacitePartyShare = value;
            case "qol.mining_glacite.shaft_party" -> miningGlaciteShaftParty = value;
            case "qol.mining_glacite.pity_chat" -> miningGlacitePityChat = value;
            case "qol.mining_glacite.enter_title" -> miningGlaciteEnterTitle = value;
            case "qol.mining_glacite.enter_chat" -> miningGlaciteEnterChat = value;
            case "qol.mining_glacite.enter_party" -> miningGlaciteEnterParty = value;
            case "qol.mining_glacite.corpse_waypoints" -> miningGlaciteCorpseWaypoints = value;
            case "qol.mining_glacite.key_announce" -> miningGlaciteKeyAnnounce = value;
            case "qol.mining_helpers.fetchur" -> miningHelpersFetchur = value;
            case "qol.mining_helpers.fossil_muncher" -> miningHelpersFossilMuncher = value;
            case "qol.mining_helpers.drill_fuel" -> miningHelpersDrillFuel = value;
            case "qol.mining_helpers.ability_hud" -> miningHelpersAbilityHud = value;
            case "qol.mining_helpers.commission_gui" -> miningHelpersCommissionGui = value;
            case "qol.mining_helpers.commission_mobs" -> miningHelpersCommissionMobs = value;
            case "qol.mining_helpers.notify_portal" -> miningHelpersNotifyPortal = value;
            case "qol.mining_helpers.notify_scrap" -> miningHelpersNotifyScrap = value;
            case "qol.mining_helpers.notify_goblin" -> miningHelpersNotifyGoblin = value;
            case "qol.mining_helpers.metal_distance" -> miningHelpersMetalDistance = value;
            case "qol.mining_helpers.detector_solver" -> miningHelpersDetectorSolver = value;
            case "qol.mining_helpers.detector_ding" -> miningHelpersDetectorDing = value;
            case "qol.mining_helpers.detector_title" -> miningHelpersDetectorTitle = value;
            case "qol.mining_helpers.red_carpets" -> miningHelpersRedCarpets = value;
            case "qol.mining_helpers.fossil_excavator" -> miningHelpersFossilExcavator = value;
            case "qol.mining_helpers.wishing_compass" -> miningHelpersWishingCompass = value;
            case "qol.mining_helpers.call_king" -> miningHelpersCallKing = value;
            case "qol.mining_helpers.break_reset" -> miningHelpersBreakReset = value;
            case "qol.mining_helpers.gemstone_desync" -> miningHelpersGemstoneDesync = value;
            case "qol.mining_hotm.sky_mall" -> miningHotmSkyMall = value;
            case "qol.mining_hotm.screen_hint" -> miningHotmScreenHint = value;
            case "qol.diana_burrows.guess" -> dianaBurrowsGuess = value;
            case "qol.diana_burrows.particles" -> dianaBurrowsParticles = value;
            case "qol.diana_burrows.waypoints" -> dianaBurrowsWaypoints = value;
            case "qol.diana_burrows.mute_spade" -> dianaBurrowsMuteSpade = value;
            case "qol.diana_burrows.fix_chat" -> dianaBurrowsFixChat = value;
            case "qol.diana_mobs.rare_esp" -> dianaMobsRareEsp = value;
            case "qol.diana_mobs.griffin_warn" -> dianaMobsGriffinWarn = value;
            case "qol.diana_profit.hud" -> dianaProfitHud = value;
            case "qol.diana_share.party" -> dianaShareParty = value;
            case "qol.diana_share.auto_warp" -> dianaShareAutoWarp = value;
            case "qol.foraging_trees.progress_hud" -> foragingTreesProgressHud = value;
            case "qol.foraging_trees.only_axe" -> foragingTreesOnlyAxe = value;
            case "qol.foraging_trees.hide_bits" -> foragingTreesHideBits = value;
            case "qol.foraging_trees.gift_hud" -> foragingTreesGiftHud = value;
            case "qol.foraging_trees.hide_unmineable" -> foragingTreesHideUnmineable = value;
            case "qol.foraging_trees.fell_title" -> foragingTreesFellTitle = value;
            case "qol.foraging_audio.mute_phantom" -> foragingAudioMutePhantom = value;
            case "qol.foraging_audio.mute_tree_break" -> foragingAudioMuteTreeBreak = value;
            case "qol.foraging_audio.mute_break_galatea" -> foragingAudioMuteBreakGalatea = value;
            case "qol.foraging_audio.mute_fusion" -> foragingAudioMuteFusion = value;
            case "qol.foraging_audio.mute_stereo" -> foragingAudioMuteStereo = value;
            case "qol.foraging_helpers.sweep_hud" -> foragingHelpersSweepHud = value;
            case "qol.foraging_helpers.temple_solver" -> foragingHelpersTempleSolver = value;
            case "qol.foraging_helpers.beacon_hints" -> foragingHelpersBeaconHints = value;
            case "qol.foraging_helpers.highlights" -> foragingHelpersHighlights = value;
            case "qol.foraging_helpers.moonglade_beacon" -> foragingHelpersMoongladeBeacon = value;
            case "qol.foraging_helpers.park_tutorial" -> foragingHelpersParkTutorial = value;
            case "qol.foraging_helpers.hunting_esp" -> foragingHelpersHuntingEsp = value;
            case "qol.foraging_helpers.frog_mask" -> foragingHelpersFrogMask = value;
            case "qol.foraging_helpers.lasso_hud" -> foragingHelpersLassoHud = value;
            case "qol.foraging_helpers.cinderbat" -> foragingHelpersCinderbat = value;
            case "qol.foraging_helpers.huntaxe_lock" -> foragingHelpersHuntaxeLock = value;
            case "qol.foraging_helpers.shard_tracker" -> foragingHelpersShardTracker = value;
            case "qol.foraging_helpers.lasso_alert" -> foragingHelpersLassoAlert = value;
            case "qol.foraging_helpers.hotf_hint" -> foragingHelpersHotfHint = value;
            case "qol.foraging_cheats.auto_beacon" -> foragingCheatsAutoBeacon = value;
            case "qol.foraging_cheats.auto_chop" -> foragingCheatsAutoChop = value;
            case "qol.foraging_cheats.axe_toss" -> foragingCheatsAxeToss = value;
            case "qol.dungeon_hud.secrets" -> dungeonHudSecrets = value;
            case "qol.dungeon_hud.score" -> dungeonHudScore = value;
            case "qol.dungeon_hud.class" -> dungeonHudClass = value;
            case "qol.dungeon_hud.floor" -> dungeonHudFloor = value;
            case "qol.dungeon_hud.cleared" -> dungeonHudCleared = value;
            case "qol.dungeon_hud.invincibility" -> dungeonHudInvincibility = value;
            case "qol.dungeon_hud.mask_overlay" -> dungeonHudMaskOverlay = value;
            case "qol.dungeon_hud.terracotta" -> dungeonHudTerracotta = value;
            case "qol.dungeon_hud.blessings" -> dungeonHudBlessings = value;
            case "qol.dungeon_hud.f7_timers" -> dungeonHudF7Timers = value;
            case "qol.dungeon_hud.ragnarock" -> dungeonHudRagnarock = value;
            case "qol.dungeon_hud.melody" -> dungeonHudMelody = value;
            case "qol.dungeon_hud.melody_other" -> dungeonHudMelodyOther = value;
            case "qol.dungeon_hud.quiz" -> dungeonHudQuiz = value;
            case "qol.dungeon_hud.map" -> dungeonHudMap = value;
            case "qol.dungeon_hud.map_doors" -> dungeonHudMapDoors = value;
            case "qol.dungeon_hud.map_players" -> dungeonHudMapPlayers = value;
            case "qol.dungeon_hud.map_extra" -> dungeonHudMapExtra = value;
            case "qol.dungeon_hud.map_hide_boss" -> dungeonHudMapHideBoss = value;
            case "qol.dungeon_hud.crypts" -> dungeonHudCrypts = value;
            case "qol.dungeon_hud.deaths" -> dungeonHudDeaths = value;
            case "qol.dungeon_hud.score_overlay" -> dungeonHudScoreOverlay = value;
            case "qol.dungeon_hud.class_icons" -> dungeonHudClassIcons = value;
            case "qol.dungeon_hud.head_markers" -> dungeonHudHeadMarkers = value;
            case "qol.dungeon_hud.room_names" -> dungeonHudRoomNames = value;
            case "qol.dungeon_hud.room_secrets" -> dungeonHudRoomSecrets = value;
            case "qol.dungeon_hud.player_names" -> dungeonHudPlayerNames = value;
            case "qol.dungeon_hud.map_mimic" -> dungeonHudMapMimic = value;
            case "qol.dungeon_hud.map_puzzles" -> dungeonHudMapPuzzles = value;
            case "qol.dungeon_hud.puzzle_timer" -> dungeonHudPuzzleTimer = value;
            case "qol.dungeon_hud.warp_cooldown" -> dungeonHudWarpCooldown = value;
            case "qol.dungeon_hud.secret_spawn" -> dungeonHudSecretSpawn = value;
            case "qol.dungeon_hud.explosive_shot" -> dungeonHudExplosiveShot = value;
            case "qol.dungeon_hud.unclaimed_chests" -> dungeonHudUnclaimedChests = value;
            case "qol.dungeon_hud.chest_warning" -> dungeonHudChestWarning = value;
            case "qol.dungeon_hud.extra_stats" -> dungeonHudExtraStats = value;
            case "qol.dungeon_hud.ledge" -> dungeonHudLedge = value;
            case "qol.dungeon_hud.ledge_yellow" -> dungeonHudLedgeYellow = value;
            case "qol.dungeon_hud.ledge_all" -> dungeonHudLedgeAll = value;
            case "qol.dungeon_hud.run_timers" -> dungeonHudRunTimers = value;
            case "qol.dungeon_hud.show_split_pbs" -> dungeonHudShowSplitPbs = value;
            case "qol.dungeon_hud.kuudra_splits" -> dungeonHudKuudraSplits = value;
            case "qol.dungeon_hud.cheater_map" -> {
                dungeonHudCheaterMap = value;
                dungeonHudMapMode = value
                        ? DungeonMapPolicy.MAP_MODE_REVEAL
                        : DungeonMapPolicy.MAP_MODE_EXPLORED;
            }
            case "qol.dungeon_hud.cheater_names" -> dungeonHudCheaterNames = value;
            case "qol.dungeon_hud.cheater_darken" -> dungeonHudCheaterDarken = value;
            case "qol.dungeon_esp.starred" -> dungeonEspStarred = value;
            case "qol.dungeon_esp.teammates" -> dungeonEspTeammates = value;
            case "qol.dungeon_esp.bats" -> dungeonEspBats = value;
            case "qol.dungeon_esp.fels" -> dungeonEspFels = value;
            case "qol.dungeon_esp.shadow" -> dungeonEspShadow = value;
            case "qol.dungeon_esp.keys" -> dungeonEspKeys = value;
            case "qol.dungeon_esp.mimic" -> dungeonEspMimic = value;
            case "qol.dungeon_esp.wither" -> dungeonEspWither = value;
            case "qol.dungeon_esp.crystals" -> dungeonEspCrystals = value;
            case "qol.dungeon_esp.secrets" -> dungeonEspSecrets = value;
            case "qol.dungeon_esp.secret_waypoints" -> dungeonEspSecretWaypoints = value;
            case "qol.dungeon_esp.hide_collected" -> dungeonEspHideCollected = value;
            case "qol.dungeon_esp.secret_clicked" -> dungeonEspSecretClicked = value;
            case "qol.dungeon_esp.secret_clicked_boss" -> dungeonEspSecretClickedBoss = value;
            case "qol.dungeon_esp.items" -> dungeonEspItems = value;
            case "qol.dungeon_esp.iced_mobs" -> dungeonEspIcedMobs = value;
            case "qol.dungeon_esp.simon" -> dungeonEspSimon = value;
            case "qol.dungeon_esp.hate_doors" -> dungeonEspHateDoors = value;
            case "qol.dungeon_esp.hate_wither" -> dungeonEspHateWither = value;
            case "qol.dungeon_esp.hate_blood" -> dungeonEspHateBlood = value;
            case "qol.dungeon_esp.hate_entrance" -> dungeonEspHateEntrance = value;
            case "qol.dungeon_esp.livid" -> dungeonEspLivid = value;
            case "qol.dungeon_esp.thorn" -> dungeonEspThorn = value;
            case "qol.dungeon_esp.spirit_bear" -> dungeonEspSpiritBear = value;
            case "qol.dungeon_esp.doors" -> dungeonEspDoors = value;
            case "qol.dungeon_esp.blood_box" -> dungeonEspBloodBox = value;
            case "qol.dungeon_esp.tracers" -> dungeonEspTracers = value;
            case "qol.dungeon_esp.depth" -> dungeonEspDepth = value;
            case "qol.dungeon_esp.ghost_block" -> dungeonEspGhostBlock = value;
            case "qol.dungeon_esp.ghost_uayor" -> dungeonEspGhostUayor = value;
            case "qol.dungeon_esp.ghost_stonk" -> dungeonEspGhostStonk = value;
            case "qol.dungeon_esp.triggerbot" -> dungeonEspTriggerBot = value;
            case "qol.dungeon_esp.trigger_crystal" -> dungeonEspTriggerCrystal = value;
            case "qol.dungeon_esp.trigger_take" -> dungeonEspTriggerTake = value;
            case "qol.dungeon_esp.trigger_place" -> dungeonEspTriggerPlace = value;
            case "qol.dungeon_esp.trigger_secret" -> dungeonEspTriggerSecret = value;
            case "qol.dungeon_esp.fill" -> dungeonEspFill = value;
            case "qol.dungeon_announce.mimic" -> dungeonAnnounceMimic = value;
            case "qol.dungeon_announce.prince" -> dungeonAnnouncePrince = value;
            case "qol.dungeon_announce.bat" -> dungeonAnnounceBat = value;
            case "qol.dungeon_announce.blood" -> dungeonAnnounceBlood = value;
            case "qol.dungeon_announce.score_title" -> dungeonAnnounceScoreTitle = value;
            case "qol.dungeon_announce.f7" -> dungeonAnnounceF7 = value;
            case "qol.dungeon_announce.ragnarock" -> dungeonAnnounceRagnarock = value;
            case "qol.dungeon_announce.rooms" -> dungeonAnnounceRooms = value;
            case "qol.dungeon_announce.melody" -> dungeonAnnounceMelody = value;
            case "qol.dungeon_announce.melody_party" -> dungeonAnnounceMelodyParty = value;
            case "qol.dungeon_announce.melody_progress" -> dungeonAnnounceMelodyProgress = value;
            case "qol.dungeon_announce.death" -> dungeonAnnounceDeath = value;
            case "qol.dungeon_announce.position" -> dungeonAnnouncePosition = value;
            case "qol.dungeon_announce.secret_chime" -> dungeonAnnounceSecretChime = value;
            case "qol.dungeon_announce.duplicate_class" -> dungeonAnnounceDuplicateClass = value;
            case "qol.dungeon_announce.player_count" -> dungeonAnnouncePlayerCount = value;
            case "qol.dungeon_announce.location" -> dungeonAnnounceLocation = value;
            case "qol.dungeon_announce.key_drop" -> dungeonAnnounceKeyDrop = value;
            case "qol.dungeon_announce.key_drop_all" -> dungeonAnnounceKeyDropAll = value;
            case "qol.dungeon_announce.auto_ult" -> dungeonAnnounceAutoUlt = value;
            case "qol.dungeon_leap.highlight" -> dungeonLeapHighlight = value;
            case "qol.dungeon_leap.custom_gui" -> dungeonLeapCustomGui = value;
            case "qol.dungeon_leap.announce" -> dungeonLeapAnnounce = value;
            case "qol.dungeon_leap.counter" -> dungeonLeapCounter = value;
            case "qol.dungeon_terminals.overlay" -> dungeonTerminalsOverlay = value;
            case "qol.dungeon_terminals.auto" -> dungeonTerminalsAuto = value;
            case "qol.dungeon_terminals.melody" -> dungeonTerminalsMelody = value;
            case "qol.dungeon_terminals.numbers" -> dungeonTerminalsNumbers = value;
            case "qol.dungeon_terminals.numbers_show" -> dungeonTerminalsNumbersShow = value;
            case "qol.dungeon_terminals.rubix" -> dungeonTerminalsRubix = value;
            case "qol.dungeon_terminals.colors" -> dungeonTerminalsColors = value;
            case "qol.dungeon_terminals.panes" -> dungeonTerminalsPanes = value;
            case "qol.dungeon_terminals.starts" -> dungeonTerminalsStarts = value;
            case "qol.dungeon_terminals.auto_melody" -> dungeonTerminalsAutoMelody = value;
            case "qol.dungeon_terminals.melody_skip" -> dungeonTerminalsMelodySkip = value;
            case "qol.dungeon_terminals.melody_skip_first_row" -> dungeonTerminalsMelodySkipFirstRow = value;
            case "qol.dungeon_terminals.sounds" -> dungeonTerminalsSounds = value;
            case "qol.dungeon_terminals.complete_sounds" -> dungeonTerminalsCompleteSounds = value;
            case "qol.dungeon_terminals.stop_tooltips" -> dungeonTerminalsStopTooltips = value;
            case "qol.dungeon_terminals.hide_clicked" -> dungeonTerminalsHideClicked = value;
            case "qol.dungeon_terminals.block_wrong_slots" -> dungeonTerminalsBlockWrongSlots = value;
            case "qol.dungeon_terminals.human_order" -> dungeonTerminalsHumanOrder = value;
            case "qol.dungeon_terminals.melody_keys" -> dungeonTerminalsMelodyKeys = value;
            case "qol.dungeon_terminals.protect" -> dungeonTerminalsProtect = value;
            case "qol.dungeon_terminals.auto_numbers" -> dungeonTerminalsAutoNumbers = value;
            case "qol.dungeon_terminals.auto_colors" -> dungeonTerminalsAutoColors = value;
            case "qol.dungeon_terminals.auto_rubix" -> dungeonTerminalsAutoRubix = value;
            case "qol.dungeon_terminals.auto_panes" -> dungeonTerminalsAutoPanes = value;
            case "qol.dungeon_terminals.auto_starts" -> dungeonTerminalsAutoStarts = value;
            case "qol.dungeon_terminals.queue" -> dungeonTerminalsQueue = value;
            case "qol.dungeon_terminals.clone" -> dungeonTerminalsClone = value;
            case "qol.dungeon_terminals.hitboxes" -> dungeonTerminalsHitboxes = value;
            case "qol.dungeon_puzzles.quiz" -> dungeonPuzzlesQuiz = value;
            case "qol.dungeon_puzzles.quiz_boxes" -> dungeonPuzzlesQuizBoxes = value;
            case "qol.dungeon_puzzles.quiz_timer" -> dungeonPuzzlesQuizTimer = value;
            case "qol.dungeon_puzzles.weirdos" -> dungeonPuzzlesWeirdos = value;
            case "qol.dungeon_puzzles.blaze" -> dungeonPuzzlesBlaze = value;
            case "qol.dungeon_puzzles.ice" -> dungeonPuzzlesIce = value;
            case "qol.dungeon_puzzles.ice_path" -> dungeonPuzzlesIcePath = value;
            case "qol.dungeon_puzzles.ice_optimize" -> dungeonPuzzlesIceOptimize = value;
            case "qol.dungeon_puzzles.water" -> dungeonPuzzlesWater = value;
            case "qol.dungeon_puzzles.water_optimized" -> dungeonPuzzlesWaterOptimized = value;
            case "qol.dungeon_puzzles.boulder" -> dungeonPuzzlesBoulder = value;
            case "qol.dungeon_puzzles.tp_maze" -> dungeonPuzzlesTpMaze = value;
            case "qol.dungeon_puzzles.creeper_beams" -> dungeonPuzzlesCreeperBeams = value;
            case "qol.dungeon_puzzles.tic_tac_toe" -> dungeonPuzzlesTicTacToe = value;
            case "qol.dungeon_f7.titles" -> dungeonF7Titles = value;
            case "qol.dungeon_f7.title_crystal" -> dungeonF7TitleCrystal = value;
            case "qol.dungeon_f7.title_wither" -> dungeonF7TitleWither = value;
            case "qol.dungeon_f7.title_terminal" -> dungeonF7TitleTerminal = value;
            case "qol.dungeon_f7.title_gate" -> dungeonF7TitleGate = value;
            case "qol.dungeon_f7.hide_other_titles" -> dungeonF7HideOtherTitles = value;
            case "qol.dungeon_f7.hide_titles_at_ss" -> dungeonF7HideTitlesAtSs = value;
            case "qol.dungeon_f7.hide_titles_at_pre4" -> dungeonF7HideTitlesAtPre4 = value;
            case "qol.dungeon_f7.hide_at_ss" -> dungeonF7HideAtSs = value;
            case "qol.dungeon_f7.hide_at_ss_pre_terms" -> dungeonF7HideAtSsPreTerms = value;
            case "qol.dungeon_f7.hide_after_leap" -> dungeonF7HideAfterLeap = value;
            case "qol.dungeon_f7.hide_after_leap_boss" -> dungeonF7HideAfterLeapBoss = value;
            case "qol.dungeon_f7.timers" -> dungeonF7Timers = value;
            case "qol.dungeon_f7.timer_ticks" -> dungeonF7TimerTicks = value;
            case "qol.dungeon_f7.timer_symbol" -> dungeonF7TimerSymbol = value;
            case "qol.dungeon_f7.timer_prefix" -> dungeonF7TimerPrefix = value;
            case "qol.dungeon_f7.timer_maxor" -> dungeonF7TimerMaxor = value;
            case "qol.dungeon_f7.timer_storm" -> dungeonF7TimerStorm = value;
            case "qol.dungeon_f7.timer_pad" -> dungeonF7TimerPad = value;
            case "qol.dungeon_f7.timer_lightning" -> dungeonF7TimerLightning = value;
            case "qol.dungeon_f7.timer_goldor" -> dungeonF7TimerGoldor = value;
            case "qol.dungeon_f7.timer_necron" -> dungeonF7TimerNecron = value;
            case "qol.dungeon_f7.crystals" -> dungeonF7Crystals = value;
            case "qol.dungeon_f7.maxor_stun" -> dungeonF7MaxorStun = value;
            case "qol.dungeon_f7.storm_crush" -> dungeonF7StormCrush = value;
            case "qol.dungeon_f7.storm_lb" -> dungeonF7StormLb = value;
            case "qol.dungeon_f7.term_start" -> dungeonF7TermStart = value;
            case "qol.dungeon_f7.crystal_spawn" -> dungeonF7CrystalSpawn = value;
            case "qol.dungeon_f7.crystal_place" -> dungeonF7CrystalPlace = value;
            case "qol.dungeon_f7.crystal_alert" -> dungeonF7CrystalAlert = value;
            case "qol.dungeon_f7.melody_display" -> dungeonF7MelodyDisplay = value;
            case "qol.dungeon_f7.wither_esp" -> dungeonF7WitherEsp = value;
            case "qol.dungeon_f7.dragons" -> dungeonF7Dragons = value;
            case "qol.dungeon_f7.dragon_spray" -> dungeonF7DragonSpray = value;
            case "qol.dungeon_f7.dragon_arrows" -> dungeonF7DragonArrows = value;
            case "qol.dungeon_f7.dragon_health" -> dungeonF7DragonHealth = value;
            case "qol.dungeon_f7.dragon_boxes" -> dungeonF7DragonBoxes = value;
            case "qol.dungeon_f7.dragon_tracers" -> dungeonF7DragonTracers = value;
            case "qol.dungeon_f7.dragon_priority" -> dungeonF7DragonPriority = value;
            case "qol.dungeon_f7.dragon_paul" -> dungeonF7DragonPaul = value;
            case "qol.dungeon_f7.p3_display" -> dungeonF7P3Display = value;
            case "qol.dungeon_f7.sharp_shooter" -> dungeonF7SharpShooter = value;
            case "qol.dungeon_f7.sharp_aim" -> dungeonF7SharpAim = value;
            case "qol.dungeon_f7.sharp_complete" -> dungeonF7SharpComplete = value;
            case "qol.dungeon_f7.term_times" -> dungeonF7TermTimes = value;
            case "qol.dungeon_f7.term_pbs" -> dungeonF7TermPbs = value;
            case "qol.dungeon_f7.predev" -> dungeonF7Predev = value;
            case "qol.dungeon_f7.predev_all" -> dungeonF7PredevAll = value;
            case "qol.dungeon_f7.ss_complete" -> dungeonF7SsComplete = value;
            case "qol.dungeon_f7.pre4_complete" -> dungeonF7Pre4Complete = value;
            case "qol.dungeon_f7.dragon_timer" -> dungeonF7DragonTimer = value;
            case "qol.dungeon_f7.goldor_frenzy" -> dungeonF7GoldorFrenzy = value;
            case "qol.dungeon_f7.purple_pad" -> dungeonF7PurplePad = value;
            case "qol.dungeon_leap.keys" -> dungeonLeapKeys = value;
            case "qol.dungeon_f7.simon" -> dungeonF7Simon = value;
            case "qol.dungeon_f7.simon_progress" -> dungeonF7SimonProgress = value;
            case "qol.dungeon_f7.simon_block_wrong" -> dungeonF7SimonBlockWrong = value;
            case "qol.dungeon_f7.simon_auto" -> dungeonF7SimonAuto = value;
            case "qol.dungeon_f7.simon_trigger" -> dungeonF7SimonTrigger = value;
            case "qol.dungeon_f7.simon_sounds" -> dungeonF7SimonSounds = value;
            case "qol.dungeon_f7.hide_diorite" -> dungeonF7HideDiorite = value;
            case "qol.dungeon_f7.arrow_align" -> dungeonF7ArrowAlign = value;
            case "qol.dungeon_f7.arrow_block_wrong" -> dungeonF7ArrowBlockWrong = value;
            case "qol.dungeon_f7.i4" -> dungeonF7I4 = value;
            case "qol.dungeon_f7.i4_predict" -> dungeonF7I4Predict = value;
            case "qol.dungeon_f7.auto_i4" -> dungeonF7AutoI4 = value;
            case "qol.dungeon_f7.auto_i4_rod" -> dungeonF7AutoI4Rod = value;
            case "qol.dungeon_f7.auto_i4_mask" -> dungeonF7AutoI4Mask = value;
            case "qol.dungeon_f7.auto_i4_leap" -> dungeonF7AutoI4Leap = value;
            case "qol.dungeon_f7.auto_i4_leap_melody" -> dungeonF7AutoI4LeapMelody = value;
            case "qol.dungeon_f7.debuff" -> dungeonF7Debuff = value;
            case "qol.dungeon_f7.debuff_auto" -> dungeonF7DebuffAuto = value;
            case "qol.dungeon_f7.debuff_ice" -> dungeonF7DebuffIce = value;
            case "qol.dungeon_f7.debuff_gravity" -> dungeonF7DebuffGravity = value;
            case "qol.dungeon_f7.gate" -> dungeonF7Gate = value;
            case "qol.dungeon_f7.relics" -> dungeonF7Relics = value;
            case "qol.dungeon_f7.relic_look" -> dungeonF7RelicLook = value;
            case "qol.dungeon_f7.relic_spawn" -> dungeonF7RelicSpawn = value;
            case "qol.dungeon_f7.relic_beacon" -> dungeonF7RelicBeacon = value;
            case "qol.dungeon_f7.relic_place" -> dungeonF7RelicPlace = value;
            case "qol.dungeon_f7.relic_highlight" -> dungeonF7RelicHighlight = value;
            case "qol.dungeon_f7.relic_block_wrong" -> dungeonF7RelicBlockWrong = value;
            case "qol.dungeon_f7.breaker_prevent_secrets" -> dungeonF7BreakerPreventSecrets = value;
            case "qol.dungeon_f7.breaker_charges" -> dungeonF7BreakerCharges = value;
            case "qol.dungeon_f7.auto_superboom" -> dungeonF7AutoSuperboom = value;
            case "qol.dungeon_f7.superboom_swap_back" -> dungeonF7SuperboomSwapBack = value;
            case "qol.dungeon_menus.salvage" -> dungeonMenusSalvage = value;
            case "qol.dungeon_menus.party_finder" -> dungeonMenusPartyFinder = value;
            case "qol.dungeon_menus.chest_profit" -> dungeonMenusChestProfit = value;
            case "qol.dungeon_menus.chest_spin" -> dungeonMenusChestSpin = value;
            case "qol.dungeon_menus.include_essence" -> dungeonMenusIncludeEssence = value;
            case "qol.dungeon_menus.include_cost" -> dungeonMenusIncludeCost = value;
            case "qol.dungeon_menus.compact_profit" -> dungeonMenusCompactProfit = value;
            case "qol.dungeon_menus.close_chest" -> dungeonMenusCloseChest = value;
            case "qol.farm_keys.lock_camera" -> farmKeysLockCamera = value;
            case "qol.dungeon_termsim.show_pbs" -> dungeonTermSimShowPbs = value;
            case "qol.camera.clip" -> cameraClip = value;
            case "qol.camera.custom_distance" -> cameraCustomDistance = value;
            case "qol.ghosts.show_powered" -> ghostsShowPowered = value;
            case "qol.slayer_display.kill_time" -> slayerDisplayKillTime = value;
            case "qol.slayer_display.dynamic_size" -> slayerDisplayDynamicSize = value;
            case "qol.slayer_time_messages.time_to_kill" -> slayerTimeMessagesTimeToKill = value;
            case "qol.slayer_time_messages.personal_best" -> slayerTimeMessagesPersonalBest = value;
            case "qol.slayer_time_messages.quest_complete" -> slayerTimeMessagesQuestComplete = value;
            case "qol.slayer_time_messages.compact" -> slayerTimeMessagesCompact = value;
            case "qol.slayer_progress.show_remaining" -> slayerProgressShowRemaining = value;
            case "qol.slayer_progress.boss_warning" -> slayerProgressBossWarning = value;
            case "qol.slayer_progress.warning_repeat" -> slayerProgressWarningRepeat = value;
            case "qol.slayer_stats.bosses_killed" -> slayerStatsBossesKilled = value;
            case "qol.slayer_stats.bosses_per_hour" -> slayerStatsBossesPerHour = value;
            case "qol.slayer_stats.average_kill_time" -> slayerStatsAverageKillTime = value;
            case "qol.slayer_stats.session_time" -> slayerStatsSessionTime = value;
            case "qol.slayer_highlights.only_mine" -> slayerHighlightsOnlyMine = value;
            case "qol.slayer_highlights.boss" -> slayerHighlightsBoss = value;
            case "qol.slayer_highlights.miniboss" -> slayerHighlightsMiniboss = value;
            case "qol.slayer_highlights.demon" -> slayerHighlightsDemon = value;
            case "qol.slayer_highlights.depth" -> slayerHighlightsDepth = value;
            case "qol.slayer_highlights.target_lines" -> slayerHighlightsTargetLines = value;
            case "qol.slayer_highlights.hide_spawn_particles" -> slayerHighlightsHideSpawnParticles = value;
            case "qol.slayer_highlights.hide_damage_splash" -> slayerHighlightsHideDamageSplash = value;
            case "qol.slayer_highlights.hide_mob_names" -> slayerHighlightsHideMobNames = value;
            case "qol.slayer_active_boss_transparency.other_players" ->
                    slayerActiveBossTransparencyPlayers = value;
            case "qol.slayer_miniboss_alert.message" -> slayerMinibossAlertMessage = value;
            case "qol.slayer_miniboss_alert.title" -> slayerMinibossAlertTitle = value;
            case "qol.slayer_drops.show_chance" -> slayerDropsShowChance = value;
            case "qol.slayer_drops.bosses_since" -> slayerDropsBossesSince = value;
            case "qol.slayer_drops.detect_automatically" -> slayerDropsDetectAutomatically = value;
            case "qol.slayer_drops.rng_hud" -> slayerDropsRngHud = value;
            case "qol.slayer_drops.rng_warn_empty" -> slayerDropsRngWarnEmpty = value;
            case "qol.slayer_drops.rng_hide_chat" -> slayerDropsRngHideChat = value;
            case "qol.slayer_drops.profit_hud" -> slayerDropsProfitHud = value;
            case "qol.slayer_drops.profit_table" -> slayerDropsProfitTable = value;
            case "qol.slayer_drops.profit_per_hour" -> slayerDropsProfitPerHour = value;
            case "qol.slayer_drops.profit_hide_outside_inventory" -> slayerDropsProfitHideOutsideInventory = value;
            case "qol.slayer_drops.ground_highlight" -> slayerDropsGroundHighlight = value;
            case "qol.slayer_drops.ground_labels" -> slayerDropsGroundLabels = value;
            case "qol.slayer_drops.price_in_chat" -> slayerDropsPriceInChat = value;
            case "qol.slayer_drops.price_title" -> slayerDropsPriceTitle = value;
            case "qol.slayer_drops.price_title_sound" -> slayerDropsPriceTitleSound = value;
            case "qol.slayer_drops.recent_highlight" -> slayerDropsRecentHighlight = value;
            case "qol.slayer_carry.announce_party" -> slayerCarryAnnounceParty = value;
            case "qol.slayer_carry.show_spawn_message" -> slayerCarryShowSpawnMessage = value;
            case "qol.slayer_carry.display" -> slayerCarryDisplay = value;
            case "qol.slayer_carry.webhook" -> slayerCarryWebhook = value;
            case "qol.slayer_carry.webhook_each" -> slayerCarryWebhookEach = value;
            case "qol.slayer_cocoon_alert.show_alert" -> slayerCocoonShowAlert = value;
            case "qol.slayer_cocoon_alert.timer" -> slayerCocoonTimer = value;
            case "qol.slayer_laser_hider.show_for_carries" -> slayerLaserShowForCarries = value;
            case "qol.slayer_attunement_display.count" -> slayerAttunementDisplayCount = value;
            case "qol.slayer_auto_soulcry.check_mana" -> slayerAutoSoulcryCheckMana = value;
            case "qol.slayer_auto_soulcry.check_hitbox" -> slayerAutoSoulcryCheckHitbox = value;
            case "qol.slayer_auto_soulcry.tick_based" -> slayerAutoSoulcryTickBased = value;
            case "qol.slayer_auto_soulcry.attack_based" -> slayerAutoSoulcryAttackBased = value;
            case "qol.slayer_auto_soulcry.other_bosses" -> slayerAutoSoulcryOtherBosses = value;
            case "qol.slayer_sounds.disable_voidgloom" -> slayerSoundsDisableVoidgloom = value;
            case "qol.slayer_sounds.disable_vampire" -> slayerSoundsDisableVampire = value;
            case "qol.slayer_voidgloom.highlight_beacon" -> slayerVoidgloomHighlightBeacon = value;
            case "qol.slayer_voidgloom.beacon_warning" -> slayerVoidgloomBeaconWarning = value;
            case "qol.slayer_voidgloom.beacon_sound" -> slayerVoidgloomBeaconSound = value;
            case "qol.slayer_voidgloom.beacon_line" -> slayerVoidgloomBeaconLine = value;
            case "qol.slayer_voidgloom.beacon_path" -> slayerVoidgloomBeaconPath = value;
            case "qol.slayer_voidgloom.beacon_timer" -> slayerVoidgloomBeaconTimer = value;
            case "qol.slayer_voidgloom.highlight_held" -> slayerVoidgloomHighlightHeld = value;
            case "qol.slayer_voidgloom.highlight_nukekubi" -> slayerVoidgloomHighlightNukekubi = value;
            case "qol.slayer_voidgloom.nukekubi_line" -> slayerVoidgloomNukekubiLine = value;
            case "qol.slayer_voidgloom.world_labels" -> slayerVoidgloomWorldLabels = value;
            case "qol.slayer_voidgloom.line_to_boss" -> slayerVoidgloomLineToBoss = value;
            case "qol.slayer_voidgloom.phase_display" -> slayerVoidgloomPhaseDisplay = value;
            case "qol.slayer_voidgloom.hits_display" -> slayerVoidgloomHitsDisplay = value;
            case "qol.slayer_voidgloom.laser_timer" -> slayerVoidgloomLaserTimer = value;
            case "qol.slayer_voidgloom.laser_health" -> slayerVoidgloomLaserHealth = value;
            case "qol.slayer_voidgloom.hide_particles" -> slayerVoidgloomHideParticles = value;
            case "qol.slayer_revenant.boom_display" -> slayerRevenantBoomDisplay = value;
            case "qol.slayer_revenant.boom_sound" -> slayerRevenantBoomSound = value;
            case "qol.slayer_revenant.boom_highlight" -> slayerRevenantBoomHighlight = value;
            case "qol.slayer_revenant.world_labels" -> slayerRevenantWorldLabels = value;
            case "qol.slayer_revenant.line_to_boss" -> slayerRevenantLineToBoss = value;
            case "qol.slayer_tarantula.highlight_egg_sacs" -> slayerTarantulaHighlightEggSacs = value;
            case "qol.slayer_tarantula.egg_hits" -> slayerTarantulaEggHits = value;
            case "qol.slayer_tarantula.highlight_invincible" -> slayerTarantulaHighlightInvincible = value;
            case "qol.slayer_tarantula.invincible_text" -> slayerTarantulaInvincibleText = value;
            case "qol.slayer_tarantula.phase_display" -> slayerTarantulaPhaseDisplay = value;
            case "qol.slayer_tarantula.world_labels" -> slayerTarantulaWorldLabels = value;
            case "qol.slayer_tarantula.mute_sounds" -> slayerTarantulaMuteSounds = value;
            case "qol.slayer_tarantula.line_to_boss" -> slayerTarantulaLineToBoss = value;
            case "qol.slayer_sven.highlight_pups" -> slayerSvenHighlightPups = value;
            case "qol.slayer_sven.pup_line" -> slayerSvenPupLine = value;
            case "qol.slayer_sven.world_labels" -> slayerSvenWorldLabels = value;
            case "qol.slayer_sven.hide_pup_nametags" -> slayerSvenHidePupNametags = value;
            case "qol.slayer_sven.howl_warning" -> slayerSvenHowlWarning = value;
            case "qol.slayer_sven.mute_sounds" -> slayerSvenMuteSounds = value;
            case "qol.slayer_sven.line_to_boss" -> slayerSvenLineToBoss = value;
            case "qol.slayer_vampire_markers.blood_ichor" -> slayerVampireMarkersBloodIchor = value;
            case "qol.slayer_vampire_markers.killer_spring" -> slayerVampireMarkersKillerSpring = value;
            case "qol.slayer_vampire_markers.twinclaws" -> slayerVampireMarkersTwinclaws = value;
            case "qol.slayer_vampire_markers.mania" -> slayerVampireMarkersMania = value;
            case "qol.slayer_vampire_markers.mania_timer" -> slayerVampireMarkersManiaTimer = value;
            case "qol.slayer_vampire_markers.steak_alert" -> slayerVampireMarkersSteakAlert = value;
            case "qol.slayer_vampire_markers.world_labels" -> slayerVampireMarkersWorldLabels = value;
            case "qol.slayer_vampire_markers.mute_sounds" -> slayerVampireMarkersMuteSounds = value;
            case "qol.slayer_vampire_markers.line_to_boss" -> slayerVampireMarkersLineToBoss = value;
            case "qol.slayer_vampire_markers.ichor_beam" -> slayerVampireMarkersIchorBeam = value;
            case "qol.slayer_vampire_markers.chalice" -> slayerVampireMarkersChalice = value;
            case "qol.slayer_vampire_markers.effigies" -> slayerVampireMarkersEffigies = value;
            case "qol.slayer_inferno.fire_pillar" -> slayerInfernoFirePillar = value;
            case "qol.slayer_inferno.fire_pillar_sound" -> slayerInfernoFirePillarSound = value;
            case "qol.slayer_inferno.fire_pits" -> slayerInfernoFirePits = value;
            case "qol.slayer_inferno.phase_display" -> slayerInfernoPhaseDisplay = value;
            case "qol.slayer_inferno.color_by_attunement" -> slayerInfernoColorByAttunement = value;
            case "qol.slayer_inferno.hide_chat" -> slayerInfernoHideChat = value;
            case "qol.slayer_inferno.hide_particles" -> slayerInfernoHideParticles = value;
            case "qol.slayer_inferno.world_labels" -> slayerInfernoWorldLabels = value;
            case "qol.slayer_inferno.line_to_boss" -> slayerInfernoLineToBoss = value;
            case "qol.slayer_inferno.gummy_warning" -> slayerInfernoGummyWarning = value;
            case "qol.slayer_quest_warning.title" -> slayerQuestWarningTitle = value;
            case "qol.slayer_quest_warning.chat" -> slayerQuestWarningChat = value;
            case "qol.slayer_auto_start.block_not_spawnable" -> slayerAutoStartBlockNotSpawnable = value;
            case "qol.slayer_vengeance.compact" -> slayerVengeanceCompact = value;
            case "qol.slayer_vengeance.use_ticks" -> slayerVengeanceUseTicks = value;
            case "qol.slayer_vengeance_damage.abbreviate" -> slayerVengeanceDamageAbbreviate = value;
            case "qol.slayer_big_drops.revenant" -> slayerBigDropsRevenant = value;
            case "qol.slayer_big_drops.tarantula" -> slayerBigDropsTarantula = value;
            case "qol.slayer_big_drops.sven" -> slayerBigDropsSven = value;
            case "qol.slayer_big_drops.voidgloom" -> slayerBigDropsVoidgloom = value;
            case "qol.slayer_big_drops.inferno" -> slayerBigDropsInferno = value;
            case "qol.slayer_big_drops.vampire" -> slayerBigDropsVampire = value;
            case "qol.storage_overlay.always_open" -> storageOverlayAlwaysOpen = value;
            case "qol.storage_overlay.outline_active" -> storageOverlayOutlineActive = value;
            case "qol.storage_overlay.inactive_tooltips" -> storageOverlayInactiveTooltips = value;
            case "qol.storage_overlay.retain_scroll" -> storageOverlayRetainScroll = value;
            case "qol.storage_overlay.invert_scroll" -> storageOverlayInvertScroll = value;
            case "qol.storage_overlay.block_item_scroll" -> storageOverlayBlockItemScroll = value;
            case "qol.storage_overlay.highlight_search" -> storageOverlayHighlightSearch = value;
            case "qol.storage_overlay.filter_search" -> storageOverlayFilterSearch = value;
            case "qol.storage_overlay.item_search" -> storageItemSearch = value;
            case "qol.storage_overlay.craft_helper" -> storageCraftHelper = value;
            case "qol.storage_overlay.museum_armor" -> storageMuseumArmor = value;
            case "qol.inventory_buttons.hover_tooltip" -> inventoryButtonsHoverTooltip = value;
            case "qol.inventory_buttons.inventory_only" -> inventoryButtonsInventoryOnly = value;
            case "qol.reward_claim.hide_chat_link" -> rewardClaimMuteChatLink = value;
            case "qol.reward_claim.block_browser" -> rewardClaimBlockBrowser = value;
            case "qol.reward_claim.wait_for_ad" -> rewardClaimWaitForAd = value;
            case "qol.freecam.show_body" -> freecamShowBody = value;
            case "qol.freecam.collide" -> freecamCollide = value;
            case "qol.hud_layout.show_background" -> hudLayoutShowBackground = value;
            case "qol.pet_hud.show_background" -> {
                HudStyleState petStyle = resolvedHudStyle("pet");
                petStyle.showBackground = value;
                putHudStyle("pet", petStyle);
            }
            case "qol.performance_hud.show_background" -> {
                HudStyleState performanceStyle = resolvedHudStyle("performance");
                performanceStyle.showBackground = value;
                putHudStyle("performance", performanceStyle);
            }
            case "qol.hud_layout.dim_unfocused" -> hudLayoutDimUnfocused = value;
            case "qol.hud_layout.hide_hotbar" -> hudHideHotbar = value;
            case "qol.hud_layout.hide_health" -> hudHideHealth = value;
            case "qol.hud_layout.hide_food" -> hudHideFood = value;
            case "qol.hud_layout.hide_armor" -> hudHideArmor = value;
            case "qol.hud_layout.hide_xp" -> hudHideXp = value;
            case "qol.hud_layout.hide_air" -> hudHideAir = value;
            case "qol.hud_layout.hide_mount" -> hudHideMount = value;
            case "qol.hud_layout.hide_scoreboard" -> hudHideScoreboard = value;
            case "qol.hud_layout.hide_boss" -> hudHideBoss = value;
            case "qol.hud_layout.hide_action" -> hudHideAction = value;
            case "qol.hud_layout.hide_item_name" -> hudHideItemName = value;
            case "qol.hud_layout.hide_effects" -> hudHideEffects = value;
            case "qol.hud_layout.hide_titles" -> hudHideTitles = value;
            case "qol.hud_layout.hide_tab" -> hudHideTab = value;
            case "qol.custom_cursor.click_anim" -> customCursorClickAnim = value;
            case "qol.custom_cursor.hold_anim" -> customCursorHoldAnim = value;
            case "qol.custom_cursor.hide_vanilla" -> customCursorHideVanilla = value;
            case "qol.legacy_textures.items" -> legacyTexturesItems = value;
            case "qol.custom_resource_pack.overworld" -> customResourcePackOverworld = value;
            case "qol.custom_resource_pack.crimson" -> customResourcePackCrimson = value;
            case "qol.custom_resource_pack.end" -> customResourcePackEnd = value;
            case "qol.custom_resource_pack.gameplay_font" -> customResourcePackGameplayFont = value;
            case "qol.render_optimizer.full_text_shadow" -> fullTextShadow = value;
            case "qol.iota.party_join_sound" -> iotaPartyJoinSound = value;
            case "qol.iota.limbo_alert" -> iotaLimboAlert = value;
            case "qol.iota.fix_fishing_hook" -> iotaFixFishingHook = value;
            case "qol.iota.mute_fishing_cast" -> iotaMuteFishingCast = value;
            case "qol.iota.mute_terminator" -> iotaMuteTerminator = value;
            case "qol.iota.party_commands" -> iotaPartyCommands = value;
            case "qol.iota.party_warp" -> iotaPartyWarp = value;
            case "qol.iota.party_transfer" -> iotaPartyTransfer = value;
            case "qol.iota.party_ping" -> iotaPartyPing = value;
            case "qol.iota.party_allinvite" -> iotaPartyAllInvite = value;
            case "qol.iota.party_tps" -> iotaPartyTps = value;
            case "qol.iota.party_promote" -> iotaPartyPromote = value;
            case "qol.iota.party_kick" -> iotaPartyKick = value;
            case "qol.iota.party_kuudra" -> iotaPartyKuudra = value;
            case "qol.iota.party_chests" -> iotaPartyChests = value;
            case "qol.iota.party_runs" -> iotaPartyRuns = value;
            case "qol.iota.party_profit" -> iotaPartyProfit = value;
            case "qol.iota.arrow_tracker" -> iotaArrowTracker = value;
            case "qol.iota.arrow_notifications" -> iotaArrowNotifications = value;
            case "qol.iota.auto_requeue" -> iotaAutoRequeue = value;
            case "qol.iota.supply_waypoints" -> iotaSupplyWaypoints = value;
            case "qol.iota.supply_hitbox" -> iotaSupplyHitbox = value;
            case "qol.iota.supply_pull_circle" -> iotaSupplyPullCircle = value;
            case "qol.iota.supply_giant_hitbox" -> iotaSupplyGiantHitbox = value;
            case "qol.iota.pile_waypoints" -> iotaPileWaypoints = value;
            case "qol.iota.pile_names" -> iotaPileNames = value;
            case "qol.iota.pearl_waypoints" -> iotaPearlWaypoints = value;
            case "qol.iota.build_waypoints" -> iotaBuildWaypoints = value;
            case "qol.iota.stun_waypoints" -> iotaStunWaypoints = value;
            case "qol.iota.ichor_pool" -> iotaIchorPool = value;
            case "qol.iota.kuudra_hitbox" -> iotaKuudraHitbox = value;
            case "qol.iota.etherwarp_helper" -> iotaEtherwarpHelper = value;
            case "qol.iota.fresh_tools" -> iotaFreshTools = value;
            case "qol.iota.fresh_announce" -> iotaFreshAnnounce = value;
            case "qol.iota.fresh_party" -> iotaFreshParty = value;
            case "qol.iota.build_info" -> iotaBuildInfo = value;
            case "qol.iota.kuudra_titles" -> iotaKuudraTitles = value;
            case "qol.stall_market.bazaar_search" -> stallBazaarSearch = value;
            case "qol.stall_market.sell_protection" -> stallSellProtection = value;
            case "qol.stall_market.angry_coop" -> stallAngryCoop = value;
            case "qol.stall_market.bin_overlay" -> stallBinOverlay = value;
            case "qol.stall_market.ah_highlight" -> stallAhHighlight = value;
            default -> {
                if (!athen().writeBoolean(settingId, value)) {
                    return false;
                }
            }
        }
        return true;
    }

    Double readNumber(String settingId) {
        if (settingId != null && settingId.startsWith("qol.custom_scoreboard.")) {
            Double value = board().readNumber(settingId);
            if (value != null) {
                return value;
            }
        }
        return switch (settingId) {
            case "qol.price_tooltips.burgers" -> (double) priceBurgerCount;
            case "qol.viewmodel.swing_speed" -> (double) viewmodelSwingSpeed;
            case "qol.viewmodel.offset_x" -> viewmodelOffsetX;
            case "qol.viewmodel.offset_y" -> viewmodelOffsetY;
            case "qol.viewmodel.offset_z" -> viewmodelOffsetZ;
            case "qol.viewmodel.scale_x" -> viewmodelScaleX;
            case "qol.viewmodel.scale_y" -> viewmodelScaleY;
            case "qol.viewmodel.scale_z" -> viewmodelScaleZ;
            case "qol.viewmodel.rot_x" -> viewmodelRotX;
            case "qol.viewmodel.rot_y" -> viewmodelRotY;
            case "qol.viewmodel.rot_z" -> viewmodelRotZ;
            case "qol.viewmodel.swing_x" -> viewmodelSwingX;
            case "qol.viewmodel.swing_y" -> viewmodelSwingY;
            case "qol.viewmodel.swing_z" -> viewmodelSwingZ;
            case "qol.item_scale.scale" -> itemScale;
            case "qol.auto_experiments.click_delay" -> (double) autoExperimentsClickDelay;
            case "qol.auto_experiments.delay_variety" -> (double) autoExperimentsDelayVariety;
            case "qol.auto_experiments.serum_count" -> (double) autoExperimentsSerumCount;
            case "qol.cheater_wardrobe.click_delay" -> (double) cheaterWardrobeClickDelay;
            case "qol.cheater_wardrobe.close_delay" -> (double) cheaterWardrobeCloseDelay;
            case "qol.cheater_wardrobe.delay_variance" -> (double) cheaterWardrobeDelayVariance;
            case "qol.auto_gfs.timer_increments" -> (double) autoGfsTimerIncrements;
            case "qol.auto_sell.delay" -> (double) autoSellDelay;
            case "qol.dungeon_terminals.delay" -> (double) dungeonTerminalsDelay;
            case "qol.dungeon_terminals.protect_ms" -> (double) dungeonTerminalsProtectMs;
            case "qol.dungeon_f7.relic_look_time" -> (double) dungeonF7RelicLookTime;
            case "qol.dungeon_f7.relic_spawn_ticks" -> (double) dungeonF7RelicSpawnTicks;
            case "qol.dungeon_f7.auto_i4_rotation" -> (double) dungeonF7AutoI4Rotation;
            case "qol.dungeon_termsim.ping" -> (double) dungeonTermSimPing;
            case "qol.dungeon_esp.trigger_delay" -> (double) dungeonEspTriggerDelay;
            case "qol.dungeon_hud.cheater_darken_factor" -> dungeonHudCheaterDarkenFactor;
            case "qol.dungeon_hud.map_scale" -> (double) dungeonHudMapScale;
            case "qol.dungeon_hud.chest_warning_count" -> (double) dungeonHudChestWarningCount;
            case "qol.dungeon_esp.secret_clicked_seconds" -> (double) dungeonEspSecretClickedSeconds;
            case "qol.dungeon_f7.superboom_delay" -> (double) dungeonF7SuperboomDelay;
            case "qol.auto_dojo.control_predict" -> (double) autoDojoControlPredict;
            case "qol.auto_dojo.mastery_delay" -> (double) autoDojoMasteryDelay;
            case "qol.fishing_creatures.timer_length" -> (double) fishingCreaturesTimerLength;
            case "qol.fishing_creatures.auto_delay" -> (double) fishingCreaturesAutoDelay;
            case "qol.dungeon_requeue.delay" -> (double) dungeonRequeueDelay;
            case "qol.dungeon_menus.party_cata" -> (double) dungeonMenusPartyCata;
            case "qol.dungeon_esp.opacity" -> (double) dungeonEspOpacity;
            case "qol.dungeon_announce.score_threshold" -> (double) dungeonAnnounceScoreThreshold;
            case "qol.auto_sell.randomization" -> (double) autoSellRandomization;
            case "qol.slayer_highlights.boss_width" -> slayerHighlightsBossWidth;
            case "qol.slayer_highlights.miniboss_width" -> slayerHighlightsMinibossWidth;
            case "qol.slayer_highlights.demon_width" -> slayerHighlightsDemonWidth;
            case "qol.slayer_highlights.target_line_width" -> slayerHighlightsTargetLineWidth;
            case "qol.slayer_highlights.target_line_distance" -> slayerHighlightsTargetLineDistance;
            case "qol.slayer_active_boss_transparency.strength" ->
                    (double) slayerActiveBossTransparencyStrength;
            case "qol.slayer_irrelevant_mobs.strength" -> (double) slayerIrrelevantMobsStrength;
            case "qol.slayer_miniboss_alert.distance" -> slayerMinibossAlertDistance;
            case "qol.slayer_cocoon_alert.pitch" -> slayerCocoonAlertPitch;
            case "qol.slayer_cocoon_alert.volume" -> slayerCocoonAlertVolume;
            case "qol.slayer_progress.warning_percent" -> (double) slayerProgressWarningPercent;
            case "qol.slayer_drops.price_title_minimum" -> (double) slayerDropsPriceTitleMinimum;
            case "qol.slayer_drops.profit_items_shown" -> (double) slayerDropsProfitItemsShown;
            case "qol.slayer_drops.ground_label_minimum" -> (double) slayerDropsGroundLabelMinimum;
            case "qol.slayer_dagger_swap.delay" -> (double) slayerDaggerSwapDelay;
            case "qol.slayer_dagger_swap.variance" -> (double) slayerDaggerSwapVariance;
            case "qol.slayer_auto_soulcry.min_delay" -> (double) slayerAutoSoulcryMinDelay;
            case "qol.slayer_auto_soulcry.max_delay" -> (double) slayerAutoSoulcryMaxDelay;
            case "qol.slayer_voidgloom.line_width" -> (double) slayerVoidgloomLineWidth;
            case "qol.slayer_voidgloom.boss_line_width" -> (double) slayerVoidgloomBossLineWidth;
            case "qol.slayer_tarantula.boss_line_width" -> (double) slayerTarantulaBossLineWidth;
            case "qol.slayer_vampire_markers.boss_line_width" -> (double) slayerVampireMarkersBossLineWidth;
            case "qol.slayer_vampire_markers.twinclaws_delay" -> (double) slayerVampireMarkersTwinclawsDelay;
            case "qol.slayer_auto_start.delay" -> (double) slayerAutoStartDelay;
            case "qol.slayer_big_drops.scale" -> slayerBigDropsScale;
            case "qol.slayer_big_drops.range" -> slayerBigDropsRange;
            case "qol.slayer_big_drops.unscale_seconds" -> (double) slayerBigDropsUnscaleSeconds;
            case "qol.storage_overlay.columns" -> (double) storageOverlayColumns;
            case "qol.storage_overlay.height" -> (double) storageOverlayHeight;
            case "qol.storage_overlay.scroll_speed" -> (double) storageOverlayScrollSpeed;
            case "qol.storage_overlay.padding" -> (double) storageOverlayPadding;
            case "qol.storage_overlay.margin" -> (double) storageOverlayMargin;
            case "qol.freecam.speed" -> freecamSpeed;
            case "qol.hud_layout.scale" -> (double) hudLayoutScale;
            case "qol.custom_cursor.size" -> customCursorSize;
            case "qol.camera.distance" -> cameraDistance;
            case "qol.foraging_helpers.sea_lumies_min" -> (double) foragingHelpersSeaLumiesMin;
            case "qol.foraging_cheats.min_cluster" -> (double) foragingCheatsMinCluster;
            case "qol.foraging_cheats.click_delay" -> (double) foragingCheatsClickDelay;
            case "qol.stall_market.sell_threshold" -> (double) stallSellThreshold;
            case "qol.render_optimizer.nether_fog_scale" -> netherFogScale;
            case "qol.render_optimizer.armor_self" -> (double) armorSelf;
            case "qol.render_optimizer.armor_others" -> (double) armorOthers;
            default -> athen().readNumber(settingId);
        };
    }

    boolean writeNumber(String settingId, double value) {
        if (settingId != null && settingId.startsWith("qol.custom_scoreboard.")
                && board().writeNumber(settingId, value)) {
            return true;
        }
        switch (settingId) {
            case "qol.price_tooltips.burgers" ->
                    priceBurgerCount = PriceTooltipsPolicy.clampBurgers((int) Math.round(value));
            case "qol.viewmodel.swing_speed" ->
                    viewmodelSwingSpeed = ViewmodelPolicy.clampSpeed((int) Math.round(value));
            case "qol.viewmodel.offset_x" -> viewmodelOffsetX = ViewmodelPolicy.clampOffset(value);
            case "qol.viewmodel.offset_y" -> viewmodelOffsetY = ViewmodelPolicy.clampOffset(value);
            case "qol.viewmodel.offset_z" -> viewmodelOffsetZ = ViewmodelPolicy.clampOffset(value);
            case "qol.viewmodel.scale_x" -> viewmodelScaleX = ViewmodelPolicy.clampScale(value);
            case "qol.viewmodel.scale_y" -> viewmodelScaleY = ViewmodelPolicy.clampScale(value);
            case "qol.viewmodel.scale_z" -> viewmodelScaleZ = ViewmodelPolicy.clampScale(value);
            case "qol.viewmodel.rot_x" -> viewmodelRotX = ViewmodelPolicy.clampRotation(value);
            case "qol.viewmodel.rot_y" -> viewmodelRotY = ViewmodelPolicy.clampRotation(value);
            case "qol.viewmodel.rot_z" -> viewmodelRotZ = ViewmodelPolicy.clampRotation(value);
            case "qol.viewmodel.swing_x" -> viewmodelSwingX = ViewmodelPolicy.clampSwing(value);
            case "qol.viewmodel.swing_y" -> viewmodelSwingY = ViewmodelPolicy.clampSwing(value);
            case "qol.viewmodel.swing_z" -> viewmodelSwingZ = ViewmodelPolicy.clampSwing(value);
            case "qol.item_scale.scale" -> itemScale = ItemScalePolicy.clamp(value);
            case "qol.auto_experiments.click_delay" -> autoExperimentsClickDelay =
                    AutoExperimentsPolicy.clampClickDelay((int) Math.round(value));
            case "qol.auto_experiments.delay_variety" -> autoExperimentsDelayVariety =
                    AutoExperimentsPolicy.clampDelayVariety((int) Math.round(value));
            case "qol.auto_experiments.serum_count" -> autoExperimentsSerumCount =
                    AutoExperimentsPolicy.clampSerumCount((int) Math.round(value));
            case "qol.cheater_wardrobe.click_delay" -> cheaterWardrobeClickDelay =
                    WardrobeKeybindPolicy.clampDelayTicks((int) Math.round(value));
            case "qol.cheater_wardrobe.close_delay" -> cheaterWardrobeCloseDelay =
                    WardrobeKeybindPolicy.clampDelayTicks((int) Math.round(value));
            case "qol.cheater_wardrobe.delay_variance" -> cheaterWardrobeDelayVariance =
                    WardrobeKeybindPolicy.clampVariance((int) Math.round(value));
            case "qol.auto_gfs.timer_increments" -> autoGfsTimerIncrements =
                    AutoGfsPolicy.clampTimerSeconds((int) Math.round(value));
            case "qol.auto_sell.delay" -> autoSellDelay =
                    AutoSellPolicy.clampDelay((int) Math.round(value));
            case "qol.dungeon_terminals.delay" -> dungeonTerminalsDelay =
                    Math.max(0, Math.min(20, (int) Math.round(value)));
            case "qol.dungeon_terminals.protect_ms" -> dungeonTerminalsProtectMs =
                    DungeonF7Policy.clampTermProtectMs((int) Math.round(value));
            case "qol.dungeon_f7.relic_look_time" -> dungeonF7RelicLookTime =
                    DungeonF7Policy.clampRelicLookMs((int) Math.round(value));
            case "qol.dungeon_f7.relic_spawn_ticks" -> dungeonF7RelicSpawnTicks =
                    DungeonF7Policy.clampRelicSpawnTicks((int) Math.round(value));
            case "qol.dungeon_f7.auto_i4_rotation" -> dungeonF7AutoI4Rotation =
                    DungeonF7Policy.clampI4RotationMs((int) Math.round(value));
            case "qol.dungeon_termsim.ping" -> dungeonTermSimPing =
                    Math.max(0, Math.min(500, (int) Math.round(value)));
            case "qol.dungeon_esp.trigger_delay" -> dungeonEspTriggerDelay =
                    DungeonLeftoverPolicy.clampTriggerDelay((int) Math.round(value));
            case "qol.dungeon_hud.cheater_darken_factor" -> dungeonHudCheaterDarkenFactor =
                    Math.max(0.0D, Math.min(1.0D, value));
            case "qol.dungeon_hud.map_scale" -> dungeonHudMapScale =
                    DungeonMapPolicy.clampHudCell((int) Math.round(value));
            case "qol.dungeon_hud.chest_warning_count" -> dungeonHudChestWarningCount =
                    DungeonBladePolicy.clampChestWarning((int) Math.round(value));
            case "qol.dungeon_esp.secret_clicked_seconds" -> dungeonEspSecretClickedSeconds =
                    TempleDungeonPolicy.clampSecretStaySeconds((int) Math.round(value));
            case "qol.dungeon_f7.superboom_delay" -> dungeonF7SuperboomDelay =
                    Math.max(1, Math.min(10, (int) Math.round(value)));
            case "qol.auto_dojo.control_predict" -> autoDojoControlPredict =
                    Math.max(1, Math.min(20, (int) Math.round(value)));
            case "qol.auto_dojo.mastery_delay" -> autoDojoMasteryDelay =
                    Math.max(0, Math.min(2000, (int) Math.round(value)));
            case "qol.fishing_creatures.timer_length" -> fishingCreaturesTimerLength =
                    FishingCreaturesPolicy.clampTimer((int) Math.round(value));
            case "qol.fishing_creatures.auto_delay" -> fishingCreaturesAutoDelay =
                    FishingCreaturesPolicy.clampAutoDelay((int) Math.round(value));
            case "qol.dungeon_requeue.delay" -> dungeonRequeueDelay =
                    Math.max(0, Math.min(200, (int) Math.round(value)));
            case "qol.dungeon_menus.party_cata" -> dungeonMenusPartyCata =
                    Math.max(0, Math.min(60, (int) Math.round(value)));
            case "qol.dungeon_esp.opacity" -> dungeonEspOpacity =
                    DungeonAssistPolicy.clampOpacity((int) Math.round(value));
            case "qol.dungeon_announce.score_threshold" -> dungeonAnnounceScoreThreshold =
                    DungeonAssistPolicy.clampScoreThreshold((int) Math.round(value));
            case "qol.auto_sell.randomization" -> autoSellRandomization =
                    AutoSellPolicy.clampRandomization((int) Math.round(value));
            case "qol.slayer_highlights.boss_width" -> slayerHighlightsBossWidth =
                    Math.max(0.5D, Math.min(6.0D, value));
            case "qol.slayer_highlights.miniboss_width" -> slayerHighlightsMinibossWidth =
                    Math.max(0.5D, Math.min(6.0D, value));
            case "qol.slayer_highlights.demon_width" -> slayerHighlightsDemonWidth =
                    Math.max(0.5D, Math.min(6.0D, value));
            case "qol.slayer_highlights.target_line_width" -> slayerHighlightsTargetLineWidth =
                    Math.max(0.5D, Math.min(6.0D, value));
            case "qol.slayer_highlights.target_line_distance" -> slayerHighlightsTargetLineDistance =
                    SlayerHighlightPolicy.clampTargetLineDistance(value);
            case "qol.slayer_active_boss_transparency.strength" ->
                    slayerActiveBossTransparencyStrength =
                            SlayerTransparencyPolicy.clampStrength((int) Math.round(value));
            case "qol.slayer_irrelevant_mobs.strength" -> slayerIrrelevantMobsStrength =
                    SlayerIrrelevantMobsPolicy.clampStrength((int) Math.round(value));
            case "qol.slayer_miniboss_alert.distance" -> slayerMinibossAlertDistance =
                    Math.max(1.0D, Math.min(64.0D, value));
            case "qol.slayer_cocoon_alert.pitch" -> slayerCocoonAlertPitch =
                    Math.max(0.0D, Math.min(2.0D, value));
            case "qol.slayer_cocoon_alert.volume" -> slayerCocoonAlertVolume =
                    Math.max(0.0D, Math.min(1.0D, value));
            case "qol.slayer_progress.warning_percent" -> slayerProgressWarningPercent =
                    SlayerProgressPolicy.clampThreshold((int) Math.round(value));
            case "qol.slayer_drops.price_title_minimum" -> slayerDropsPriceTitleMinimum =
                    Math.max(100_000, Math.min(100_000_000, (int) Math.round(value)));
            case "qol.slayer_drops.profit_items_shown" -> slayerDropsProfitItemsShown =
                    Math.max(1, Math.min(10, (int) Math.round(value)));
            case "qol.slayer_drops.ground_label_minimum" -> slayerDropsGroundLabelMinimum =
                    (int) SlayerGroundDropPolicy.clampMinimum(Math.round(value));
            case "qol.slayer_dagger_swap.delay" -> slayerDaggerSwapDelay =
                    SlayerMechanicsPolicy.clampDelay((int) Math.round(value));
            case "qol.slayer_dagger_swap.variance" -> slayerDaggerSwapVariance =
                    SlayerMechanicsPolicy.clampVariance((int) Math.round(value));
            case "qol.slayer_auto_soulcry.min_delay" -> slayerAutoSoulcryMinDelay =
                    SlayerMechanicsPolicy.clampSoulcryDelay((int) Math.round(value));
            case "qol.slayer_auto_soulcry.max_delay" -> slayerAutoSoulcryMaxDelay =
                    SlayerMechanicsPolicy.clampSoulcryDelay((int) Math.round(value));
            case "qol.slayer_voidgloom.line_width" -> slayerVoidgloomLineWidth =
                    SlayerFightPolicy.clampLineWidth((int) Math.round(value));
            case "qol.slayer_voidgloom.boss_line_width" -> slayerVoidgloomBossLineWidth =
                    SlayerFightPolicy.clampLineWidth((int) Math.round(value));
            case "qol.slayer_tarantula.boss_line_width" -> slayerTarantulaBossLineWidth =
                    SlayerFightPolicy.clampLineWidth((int) Math.round(value));
            case "qol.slayer_vampire_markers.boss_line_width" -> slayerVampireMarkersBossLineWidth =
                    SlayerFightPolicy.clampLineWidth((int) Math.round(value));
            case "qol.slayer_vampire_markers.twinclaws_delay" -> slayerVampireMarkersTwinclawsDelay =
                    SlayerFightPolicy.clampTwinclawsDelay((int) Math.round(value));
            case "qol.slayer_auto_start.delay" -> slayerAutoStartDelay =
                    SlayerFightPolicy.clampAutoStartDelayTicks((int) Math.round(value));
            case "qol.slayer_big_drops.scale" -> slayerBigDropsScale =
                    SlayerDropScalePolicy.clampScale(value);
            case "qol.slayer_big_drops.range" -> slayerBigDropsRange =
                    SlayerDropScalePolicy.clampRangeMultiplier(value);
            case "qol.slayer_big_drops.unscale_seconds" -> slayerBigDropsUnscaleSeconds =
                    SlayerDropScalePolicy.clampUnscaleSeconds((int) Math.round(value));
            case "qol.storage_overlay.columns" -> storageOverlayColumns =
                    StorageOverlayPolicy.clampColumns((int) Math.round(value));
            case "qol.storage_overlay.height" -> storageOverlayHeight =
                    StorageOverlayPolicy.clampHeight((int) Math.round(value));
            case "qol.storage_overlay.scroll_speed" -> storageOverlayScrollSpeed =
                    StorageOverlayPolicy.clampScrollSpeed((int) Math.round(value));
            case "qol.storage_overlay.padding" -> storageOverlayPadding =
                    StorageOverlayPolicy.clampSpacing((int) Math.round(value));
            case "qol.storage_overlay.margin" -> storageOverlayMargin =
                    StorageOverlayPolicy.clampSpacing((int) Math.round(value));
            case "qol.freecam.speed" -> freecamSpeed = FreecamPolicy.clampSpeed(value);
            case "qol.hud_layout.scale" ->
                    hudLayoutScale = HudStylePolicy.clampScale((float) value);
            case "qol.custom_cursor.size" ->
                    customCursorSize = CustomCursorPolicy.clampSize(value);
            case "qol.camera.distance" ->
                    cameraDistance = TempleDungeonPolicy.clampCameraDistance(value);
            case "qol.foraging_helpers.sea_lumies_min" -> foragingHelpersSeaLumiesMin =
                    Math.max(1, Math.min(4, (int) Math.round(value)));
            case "qol.foraging_cheats.min_cluster" -> foragingCheatsMinCluster =
                    Math.max(1, Math.min(35, (int) Math.round(value)));
            case "qol.foraging_cheats.click_delay" -> foragingCheatsClickDelay =
                    Math.max(1, Math.min(20, (int) Math.round(value)));
            case "qol.stall_market.sell_threshold" ->
                    stallSellThreshold = StallMarketPolicy.clampSellThreshold(Math.round(value));
            case "qol.render_optimizer.nether_fog_scale" ->
                    netherFogScale = SkyBlockUtilityPolicy.clampFogScale(value);
            case "qol.render_optimizer.armor_self" ->
                    armorSelf = SkyBlockUtilityPolicy.clampArmorPercent((int) Math.round(value));
            case "qol.render_optimizer.armor_others" ->
                    armorOthers = SkyBlockUtilityPolicy.clampArmorPercent((int) Math.round(value));
            default -> {
                if (!athen().writeNumber(settingId, value)) {
                    return false;
                }
            }
        }
        return true;
    }

    String readEnum(String settingId) {
        if (settingId != null && settingId.startsWith("qol.custom_scoreboard.")) {
            String value = board().readEnum(settingId);
            if (value != null) {
                return value;
            }
        }
        if ("qol.dungeon_hud.map_mode".equals(settingId)) {
            return dungeonMapMode();
        }
        if ("qol.render_optimizer.vignette".equals(settingId)) {
            return normalizeVignette(vignetteMode);
        }
        if ("qol.render_optimizer.hide_island_clouds".equals(settingId)) {
            return SkyBlockUtilityPolicy.normalizeCloudMode(hideIslandClouds);
        }
        if ("qol.auto_sell.click_type".equals(settingId)) {
            return AutoSellPolicy.normalizeClickType(autoSellClickType);
        }
        if ("qol.dungeon_esp.hate_wither_glass".equals(settingId)) {
            return EmberDungeonPolicy.prettyGlass(EmberDungeonPolicy.glassTint(dungeonEspHateWitherGlass));
        }
        if ("qol.dungeon_esp.hate_blood_glass".equals(settingId)) {
            return EmberDungeonPolicy.prettyGlass(EmberDungeonPolicy.glassTint(dungeonEspHateBloodGlass));
        }
        if ("qol.dungeon_esp.hate_entrance_glass".equals(settingId)) {
            return EmberDungeonPolicy.prettyGlass(EmberDungeonPolicy.glassTint(dungeonEspHateEntranceGlass));
        }
        if ("qol.dungeon_terminals.melody_skip_mode".equals(settingId)) {
            return DungeonPolicy.normalizeMelodySkipMode(dungeonTerminalsMelodySkipMode);
        }
        if ("qol.dungeon_f7.auto_i4_leap_class".equals(settingId)) {
            return DungeonPolicy.normalizeI4LeapClass(dungeonF7AutoI4LeapClass);
        }
        if ("qol.dungeon_menus.close_chest.mode".equals(settingId)) {
            return DungeonF7Policy.normalizeCloseChestMode(dungeonMenusCloseChestMode);
        }
        if ("qol.fishing_creatures.min_rarity".equals(settingId)) {
            return FishingCreaturesPolicy.normalizeRarity(fishingCreaturesMinRarity);
        }
        if ("qol.fishing_trophy.min_rarity".equals(settingId)) {
            return FishingTrophyPolicy.normalizeRarity(fishingTrophyMinRarity);
        }
        if ("qol.iota.arrow_visibility".equals(settingId)) {
            return IotaPolicy.normalizeVisibility(iotaArrowVisibility);
        }
        if ("qol.iota.stun_pod".equals(settingId)) {
            return IotaKuudraPolicy.normalizeStunPod(iotaStunPod);
        }
        if ("qol.ghosts.highlight_style".equals(settingId)) {
            return GhostsPolicy.normalizeHighlight(ghostsHighlightStyle);
        }
        if ("qol.dungeon_f7.dragon_solo_class".equals(settingId)) {
            return DungeonF7Policy.normalizeSoloClass(dungeonF7DragonSoloClass);
        }
        return athen().readEnum(settingId);
    }

    boolean writeEnum(String settingId, String value) {
        if (settingId != null && settingId.startsWith("qol.custom_scoreboard.")
                && board().writeEnum(settingId, value)) {
            return true;
        }
        if ("qol.dungeon_hud.map_mode".equals(settingId)) {
            applyDungeonMapMode(value);
            return true;
        }
        if ("qol.render_optimizer.vignette".equals(settingId)) {
            vignetteMode = normalizeVignette(value);
            return true;
        }
        if ("qol.render_optimizer.hide_island_clouds".equals(settingId)) {
            hideIslandClouds = SkyBlockUtilityPolicy.normalizeCloudMode(value);
            return true;
        }
        if ("qol.auto_sell.click_type".equals(settingId)) {
            autoSellClickType = AutoSellPolicy.normalizeClickType(value);
            return true;
        }
        if ("qol.dungeon_esp.hate_wither_glass".equals(settingId)) {
            dungeonEspHateWitherGlass = EmberDungeonPolicy.prettyGlass(EmberDungeonPolicy.glassTint(value));
            return true;
        }
        if ("qol.dungeon_esp.hate_blood_glass".equals(settingId)) {
            dungeonEspHateBloodGlass = EmberDungeonPolicy.prettyGlass(EmberDungeonPolicy.glassTint(value));
            return true;
        }
        if ("qol.dungeon_esp.hate_entrance_glass".equals(settingId)) {
            dungeonEspHateEntranceGlass = EmberDungeonPolicy.prettyGlass(EmberDungeonPolicy.glassTint(value));
            return true;
        }
        if ("qol.dungeon_terminals.melody_skip_mode".equals(settingId)) {
            dungeonTerminalsMelodySkipMode = DungeonPolicy.normalizeMelodySkipMode(value);
            return true;
        }
        if ("qol.dungeon_f7.auto_i4_leap_class".equals(settingId)) {
            dungeonF7AutoI4LeapClass = DungeonPolicy.normalizeI4LeapClass(value);
            return true;
        }
        if ("qol.dungeon_menus.close_chest.mode".equals(settingId)) {
            dungeonMenusCloseChestMode = DungeonF7Policy.normalizeCloseChestMode(value);
            return true;
        }
        if ("qol.fishing_creatures.min_rarity".equals(settingId)) {
            fishingCreaturesMinRarity = FishingCreaturesPolicy.normalizeRarity(value);
            return true;
        }
        if ("qol.fishing_trophy.min_rarity".equals(settingId)) {
            fishingTrophyMinRarity = FishingTrophyPolicy.normalizeRarity(value);
            return true;
        }
        if ("qol.iota.arrow_visibility".equals(settingId)) {
            iotaArrowVisibility = IotaPolicy.normalizeVisibility(value);
            return true;
        }
        if ("qol.iota.stun_pod".equals(settingId)) {
            iotaStunPod = IotaKuudraPolicy.normalizeStunPod(value);
            return true;
        }
        if ("qol.ghosts.highlight_style".equals(settingId)) {
            ghostsHighlightStyle = GhostsPolicy.normalizeHighlight(value);
            return true;
        }
        if ("qol.dungeon_f7.dragon_solo_class".equals(settingId)) {
            dungeonF7DragonSoloClass = DungeonF7Policy.normalizeSoloClass(value);
            return true;
        }
        return athen().writeEnum(settingId, value);
    }

    String dungeonMapMode() {
        if (dungeonHudCheaterMap) {
            return DungeonMapPolicy.MAP_MODE_REVEAL;
        }
        return DungeonMapPolicy.normalizeMapMode(dungeonHudMapMode);
    }

    boolean dungeonMapRevealHidden() {
        return DungeonMapPolicy.revealsHidden(dungeonMapMode());
    }

    private void applyDungeonMapMode(String value) {
        dungeonHudMapMode = DungeonMapPolicy.normalizeMapMode(value);
        dungeonHudCheaterMap = DungeonMapPolicy.revealsHidden(dungeonHudMapMode);
    }

    Integer readColor(String settingId) {
        if (settingId != null && settingId.startsWith("qol.custom_scoreboard.")) {
            Integer value = board().readColor(settingId);
            if (value != null) {
                return value;
            }
        }
        return switch (settingId) {
            case "qol.active_pet_highlight.color" -> activePetHighlightColor;
            case "qol.anvil_helper.color" -> anvilHelperColor;
            case "qol.experiment_solver.first_color" -> experimentFirstColor;
            case "qol.experiment_solver.second_color" -> experimentSecondColor;
            case "qol.experiment_solver.matched_color" -> experimentMatchedColor;
            case "qol.experiment_solver.match_color" -> experimentMatchColor;
            case "qol.experiment_solver.powerup_color" -> experimentPowerupColor;
            case "qol.slayer_highlights.boss_color" -> slayerHighlightsBossColor;
            case "qol.slayer_highlights.miniboss_color" -> slayerHighlightsMinibossColor;
            case "qol.slayer_highlights.demon_color" -> slayerHighlightsDemonColor;
            case "qol.slayer_voidgloom.beacon_color" -> slayerVoidgloomBeaconColor;
            case "qol.slayer_voidgloom.line_color" -> slayerVoidgloomLineColor;
            case "qol.slayer_voidgloom.nukekubi_color" -> slayerVoidgloomNukekubiColor;
            case "qol.slayer_tarantula.egg_color" -> slayerTarantulaEggColor;
            case "qol.slayer_tarantula.invincible_color" -> slayerTarantulaInvincibleColor;
            case "qol.slayer_revenant.boom_color" -> slayerRevenantBoomColor;
            case "qol.slayer_sven.pup_color" -> slayerSvenPupColor;
            case "qol.slayer_vampire_markers.ichor_color" -> slayerVampireMarkersIchorColor;
            case "qol.slayer_vampire_markers.spring_color" -> slayerVampireMarkersSpringColor;
            case "qol.slayer_vampire_markers.steak_color" -> slayerVampireMarkersSteakColor;
            case "qol.slayer_vampire_markers.chalice_color" -> slayerVampireMarkersChaliceColor;
            case "qol.slayer_inferno.pillar_color" -> slayerInfernoPillarColor;
            case "qol.storage_overlay.outline_color" -> storageOverlayOutlineColor;
            case "qol.storage_overlay.highlight_color" -> storageOverlayHighlightColor;
            case "qol.storage_overlay.panel_color" -> storageOverlayPanelColor;
            case "qol.storage_overlay.card_color" -> storageOverlayCardColor;
            case "qol.storage_overlay.card_active_color" -> storageOverlayCardActiveColor;
            case "qol.storage_overlay.player_color" -> storageOverlayPlayerColor;
            case "qol.dungeon_esp.starred_color" -> dungeonEspStarredColor;
            case "qol.dungeon_esp.bat_color" -> dungeonEspBatColor;
            case "qol.dungeon_esp.fel_color" -> dungeonEspFelColor;
            case "qol.dungeon_esp.key_color" -> dungeonEspKeyColor;
            case "qol.dungeon_esp.blood_key_color" -> dungeonEspBloodKeyColor;
            case "qol.dungeon_esp.shadow_color" -> dungeonEspShadowColor;
            case "qol.dungeon_esp.teammate_color" -> dungeonEspTeammateColor;
            case "qol.dungeon_esp.mimic_color" -> dungeonEspMimicColor;
            case "qol.dungeon_esp.wither_color" -> dungeonEspWitherColor;
            case "qol.dungeon_esp.crystal_color" -> dungeonEspCrystalColor;
            case "qol.dungeon_esp.secret_color" -> dungeonEspSecretColor;
            case "qol.dungeon_esp.chest_color" -> dungeonEspChestColor;
            case "qol.dungeon_esp.secret_clicked_color" -> dungeonEspSecretClickedColor;
            case "qol.dungeon_esp.secret_clicked_locked_color" -> dungeonEspSecretClickedLockedColor;
            case "qol.dungeon_esp.simon_color" -> dungeonEspSimonColor;
            case "qol.dungeon_esp.livid_color" -> dungeonEspLividColor;
            case "qol.dungeon_esp.thorn_color" -> dungeonEspThornColor;
            case "qol.dungeon_esp.spirit_bear_color" -> dungeonEspSpiritBearColor;
            case "qol.dungeon_esp.blood_box_color" -> dungeonEspBloodBoxColor;
            case "qol.dungeon_esp.blood_line_color" -> dungeonEspBloodLineColor;
            case "qol.dungeon_terminals.color" -> dungeonTerminalsColor;
            case "qol.dungeon_terminals.hitbox_color" -> dungeonTerminalsHitboxColor;
            case "qol.dungeon_terminals.numbers_1_color" -> dungeonTerminalsNumbers1Color;
            case "qol.dungeon_terminals.numbers_2_color" -> dungeonTerminalsNumbers2Color;
            case "qol.dungeon_terminals.numbers_3_color" -> dungeonTerminalsNumbers3Color;
            case "qol.dungeon_terminals.rubix_pos_color" -> dungeonTerminalsRubixPosColor;
            case "qol.dungeon_terminals.rubix_neg_color" -> dungeonTerminalsRubixNegColor;
            case "qol.dungeon_terminals.melody_column_color" -> dungeonTerminalsMelodyColumnColor;
            case "qol.dungeon_terminals.melody_indicator_color" -> dungeonTerminalsMelodyIndicatorColor;
            case "qol.dungeon_terminals.melody_wrong_color" -> dungeonTerminalsMelodyWrongColor;
            case "qol.dungeon_f7.gate_color" -> dungeonF7GateColor;
            case "qol.dungeon_f7.i4_color" -> dungeonF7I4Color;
            case "qol.dungeon_f7.i4_predict_color" -> dungeonF7I4PredictColor;
            case "qol.dungeon_f7.sharp_marked_color" -> dungeonF7SharpMarkedColor;
            case "qol.dungeon_f7.sharp_target_color" -> dungeonF7SharpTargetColor;
            case "qol.dungeon_f7.sharp_aim1_color" -> dungeonF7SharpAim1Color;
            case "qol.dungeon_f7.sharp_aim2_color" -> dungeonF7SharpAim2Color;
            case "qol.dungeon_f7.sharp_aim3_color" -> dungeonF7SharpAim3Color;
            case "qol.dungeon_f7.maxor_color" -> dungeonF7MaxorColor;
            case "qol.dungeon_f7.storm_color" -> dungeonF7StormColor;
            case "qol.dungeon_f7.goldor_color" -> dungeonF7GoldorColor;
            case "qol.dungeon_f7.necron_color" -> dungeonF7NecronColor;
            case "qol.dungeon_f7.simon_first_color" -> dungeonF7SimonFirstColor;
            case "qol.dungeon_f7.simon_second_color" -> dungeonF7SimonSecondColor;
            case "qol.dungeon_f7.simon_other_color" -> dungeonF7SimonOtherColor;
            case "qol.dungeon_hud.mask_overlay_color" -> dungeonHudMaskOverlayColor;
            case "qol.dungeon_menus.salvage50_color" -> dungeonMenusSalvage50Color;
            case "qol.dungeon_menus.salvage_low_color" -> dungeonMenusSalvageLowColor;
            case "qol.dungeon_menus.profit_color" -> dungeonMenusProfitColor;
            case "qol.fishing_creatures.esp_color" -> fishingCreaturesEspColor;
            case "qol.fishing_hotspots.color" -> fishingHotspotsColor;
            case "qol.diana_burrows.guess_color" -> dianaBurrowsGuessColor;
            case "qol.diana_burrows.start_color" -> dianaBurrowsStartColor;
            case "qol.diana_burrows.mob_color" -> dianaBurrowsMobColor;
            case "qol.diana_burrows.treasure_color" -> dianaBurrowsTreasureColor;
            case "qol.diana_mobs.esp_color" -> dianaMobsEspColor;
            case "qol.hud_layout.background" -> hudLayoutBackgroundColor;
            case "qol.hud_layout.text" -> hudLayoutTextColor;
            case "qol.custom_cursor.fill" -> customCursorFill;
            case "qol.custom_cursor.outline" -> customCursorOutline;
            case "qol.custom_cursor.accent" -> customCursorAccent;
            case "qol.ghosts.fill_color" -> ghostsFillColor;
            case "qol.ghosts.outline_color" -> ghostsOutlineColor;
            default -> athen().readColor(settingId);
        };
    }

    boolean writeColor(String settingId, int argb) {
        if (settingId != null && settingId.startsWith("qol.custom_scoreboard.")
                && board().writeColor(settingId, argb)) {
            return true;
        }
        switch (settingId) {
            case "qol.active_pet_highlight.color" -> activePetHighlightColor = argb;
            case "qol.anvil_helper.color" -> anvilHelperColor = argb;
            case "qol.experiment_solver.first_color" -> experimentFirstColor = argb;
            case "qol.experiment_solver.second_color" -> experimentSecondColor = argb;
            case "qol.experiment_solver.matched_color" -> experimentMatchedColor = argb;
            case "qol.experiment_solver.match_color" -> experimentMatchColor = argb;
            case "qol.experiment_solver.powerup_color" -> experimentPowerupColor = argb;
            case "qol.slayer_highlights.boss_color" -> slayerHighlightsBossColor = argb;
            case "qol.slayer_highlights.miniboss_color" -> slayerHighlightsMinibossColor = argb;
            case "qol.slayer_highlights.demon_color" -> slayerHighlightsDemonColor = argb;
            case "qol.slayer_voidgloom.beacon_color" -> slayerVoidgloomBeaconColor = argb;
            case "qol.slayer_voidgloom.line_color" -> slayerVoidgloomLineColor = argb;
            case "qol.slayer_voidgloom.nukekubi_color" -> slayerVoidgloomNukekubiColor = argb;
            case "qol.slayer_tarantula.egg_color" -> slayerTarantulaEggColor = argb;
            case "qol.slayer_tarantula.invincible_color" -> slayerTarantulaInvincibleColor = argb;
            case "qol.slayer_revenant.boom_color" -> slayerRevenantBoomColor = argb;
            case "qol.slayer_sven.pup_color" -> slayerSvenPupColor = argb;
            case "qol.slayer_vampire_markers.ichor_color" -> slayerVampireMarkersIchorColor = argb;
            case "qol.slayer_vampire_markers.spring_color" -> slayerVampireMarkersSpringColor = argb;
            case "qol.slayer_vampire_markers.steak_color" -> slayerVampireMarkersSteakColor = argb;
            case "qol.slayer_vampire_markers.chalice_color" -> slayerVampireMarkersChaliceColor = argb;
            case "qol.slayer_inferno.pillar_color" -> slayerInfernoPillarColor = argb;
            case "qol.storage_overlay.outline_color" -> storageOverlayOutlineColor = argb;
            case "qol.storage_overlay.highlight_color" -> storageOverlayHighlightColor = argb;
            case "qol.storage_overlay.panel_color" -> storageOverlayPanelColor = argb;
            case "qol.storage_overlay.card_color" -> storageOverlayCardColor = argb;
            case "qol.storage_overlay.card_active_color" -> storageOverlayCardActiveColor = argb;
            case "qol.storage_overlay.player_color" -> storageOverlayPlayerColor = argb;
            case "qol.dungeon_esp.starred_color" -> dungeonEspStarredColor = argb;
            case "qol.dungeon_esp.bat_color" -> dungeonEspBatColor = argb;
            case "qol.dungeon_esp.fel_color" -> dungeonEspFelColor = argb;
            case "qol.dungeon_esp.key_color" -> dungeonEspKeyColor = argb;
            case "qol.dungeon_esp.blood_key_color" -> dungeonEspBloodKeyColor = argb;
            case "qol.dungeon_esp.shadow_color" -> dungeonEspShadowColor = argb;
            case "qol.dungeon_esp.teammate_color" -> dungeonEspTeammateColor = argb;
            case "qol.dungeon_esp.mimic_color" -> dungeonEspMimicColor = argb;
            case "qol.dungeon_esp.wither_color" -> dungeonEspWitherColor = argb;
            case "qol.dungeon_esp.crystal_color" -> dungeonEspCrystalColor = argb;
            case "qol.dungeon_esp.secret_color" -> dungeonEspSecretColor = argb;
            case "qol.dungeon_esp.chest_color" -> dungeonEspChestColor = argb;
            case "qol.dungeon_esp.secret_clicked_color" -> dungeonEspSecretClickedColor = argb;
            case "qol.dungeon_esp.secret_clicked_locked_color" -> dungeonEspSecretClickedLockedColor = argb;
            case "qol.dungeon_esp.simon_color" -> dungeonEspSimonColor = argb;
            case "qol.dungeon_esp.livid_color" -> dungeonEspLividColor = argb;
            case "qol.dungeon_esp.thorn_color" -> dungeonEspThornColor = argb;
            case "qol.dungeon_esp.spirit_bear_color" -> dungeonEspSpiritBearColor = argb;
            case "qol.dungeon_esp.blood_box_color" -> dungeonEspBloodBoxColor = argb;
            case "qol.dungeon_esp.blood_line_color" -> dungeonEspBloodLineColor = argb;
            case "qol.dungeon_terminals.color" -> dungeonTerminalsColor = argb;
            case "qol.dungeon_terminals.hitbox_color" -> dungeonTerminalsHitboxColor = argb;
            case "qol.dungeon_terminals.numbers_1_color" -> dungeonTerminalsNumbers1Color = argb;
            case "qol.dungeon_terminals.numbers_2_color" -> dungeonTerminalsNumbers2Color = argb;
            case "qol.dungeon_terminals.numbers_3_color" -> dungeonTerminalsNumbers3Color = argb;
            case "qol.dungeon_terminals.rubix_pos_color" -> dungeonTerminalsRubixPosColor = argb;
            case "qol.dungeon_terminals.rubix_neg_color" -> dungeonTerminalsRubixNegColor = argb;
            case "qol.dungeon_terminals.melody_column_color" -> dungeonTerminalsMelodyColumnColor = argb;
            case "qol.dungeon_terminals.melody_indicator_color" -> dungeonTerminalsMelodyIndicatorColor = argb;
            case "qol.dungeon_terminals.melody_wrong_color" -> dungeonTerminalsMelodyWrongColor = argb;
            case "qol.dungeon_f7.gate_color" -> dungeonF7GateColor = argb;
            case "qol.dungeon_f7.i4_color" -> dungeonF7I4Color = argb;
            case "qol.dungeon_f7.i4_predict_color" -> dungeonF7I4PredictColor = argb;
            case "qol.dungeon_f7.sharp_marked_color" -> dungeonF7SharpMarkedColor = argb;
            case "qol.dungeon_f7.sharp_target_color" -> dungeonF7SharpTargetColor = argb;
            case "qol.dungeon_f7.sharp_aim1_color" -> dungeonF7SharpAim1Color = argb;
            case "qol.dungeon_f7.sharp_aim2_color" -> dungeonF7SharpAim2Color = argb;
            case "qol.dungeon_f7.sharp_aim3_color" -> dungeonF7SharpAim3Color = argb;
            case "qol.dungeon_f7.maxor_color" -> dungeonF7MaxorColor = argb;
            case "qol.dungeon_f7.storm_color" -> dungeonF7StormColor = argb;
            case "qol.dungeon_f7.goldor_color" -> dungeonF7GoldorColor = argb;
            case "qol.dungeon_f7.necron_color" -> dungeonF7NecronColor = argb;
            case "qol.dungeon_f7.simon_first_color" -> dungeonF7SimonFirstColor = argb;
            case "qol.dungeon_f7.simon_second_color" -> dungeonF7SimonSecondColor = argb;
            case "qol.dungeon_f7.simon_other_color" -> dungeonF7SimonOtherColor = argb;
            case "qol.dungeon_hud.mask_overlay_color" -> dungeonHudMaskOverlayColor = argb;
            case "qol.dungeon_menus.salvage50_color" -> dungeonMenusSalvage50Color = argb;
            case "qol.dungeon_menus.salvage_low_color" -> dungeonMenusSalvageLowColor = argb;
            case "qol.dungeon_menus.profit_color" -> dungeonMenusProfitColor = argb;
            case "qol.fishing_creatures.esp_color" -> fishingCreaturesEspColor = argb;
            case "qol.fishing_hotspots.color" -> fishingHotspotsColor = argb;
            case "qol.diana_burrows.guess_color" -> dianaBurrowsGuessColor = argb;
            case "qol.diana_burrows.start_color" -> dianaBurrowsStartColor = argb;
            case "qol.diana_burrows.mob_color" -> dianaBurrowsMobColor = argb;
            case "qol.diana_burrows.treasure_color" -> dianaBurrowsTreasureColor = argb;
            case "qol.diana_mobs.esp_color" -> dianaMobsEspColor = argb;
            case "qol.hud_layout.background" -> hudLayoutBackgroundColor = argb;
            case "qol.hud_layout.text" -> hudLayoutTextColor = argb;
            case "qol.custom_cursor.fill" -> customCursorFill = argb;
            case "qol.custom_cursor.outline" -> customCursorOutline = argb;
            case "qol.custom_cursor.accent" -> customCursorAccent = argb;
            case "qol.ghosts.fill_color" -> ghostsFillColor = argb;
            case "qol.ghosts.outline_color" -> ghostsOutlineColor = argb;
            default -> {
                if (!athen().writeColor(settingId, argb)) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean resetModule(String moduleId) {
        QolSkyblockExtras d = new QolSkyblockExtras();
        switch (moduleId) {
            case "qol.info_tooltips" -> {
                infoTooltipsEnabled = d.infoTooltipsEnabled;
                infoDungeonQuality = d.infoDungeonQuality;
                infoCreatedDate = d.infoCreatedDate;
                infoHexColor = d.infoHexColor;
                infoMuseum = d.infoMuseum;
                infoItemId = d.infoItemId;
                infoStarCount = d.infoStarCount;
                infoPetCandy = d.infoPetCandy;
                infoRevertMasterStars = d.infoRevertMasterStars;
                athen().copyModule(moduleId, d.athen());
            }
            case "qol.price_tooltips" -> {
                priceTooltipsEnabled = d.priceTooltipsEnabled;
                priceLowestBin = d.priceLowestBin;
                priceBazaar = d.priceBazaar;
                priceNpc = d.priceNpc;
                priceMotes = d.priceMotes;
                pricePaid = d.pricePaid;
                priceBurgerCount = d.priceBurgerCount;
            }
            case "qol.viewmodel" -> {
                viewmodelEnabled = d.viewmodelEnabled;
                viewmodelNoHaste = d.viewmodelNoHaste;
                viewmodelNoEquip = d.viewmodelNoEquip;
                viewmodelNoBowSwing = d.viewmodelNoBowSwing;
                viewmodelApplyToHand = d.viewmodelApplyToHand;
                viewmodelSwingSpeed = d.viewmodelSwingSpeed;
                viewmodelOffsetX = d.viewmodelOffsetX;
                viewmodelOffsetY = d.viewmodelOffsetY;
                viewmodelOffsetZ = d.viewmodelOffsetZ;
                viewmodelScaleX = d.viewmodelScaleX;
                viewmodelScaleY = d.viewmodelScaleY;
                viewmodelScaleZ = d.viewmodelScaleZ;
                viewmodelRotX = d.viewmodelRotX;
                viewmodelRotY = d.viewmodelRotY;
                viewmodelRotZ = d.viewmodelRotZ;
                viewmodelSwingX = d.viewmodelSwingX;
                viewmodelSwingY = d.viewmodelSwingY;
                viewmodelSwingZ = d.viewmodelSwingZ;
            }
            case "qol.animation_fix" -> {
                animationFixEnabled = d.animationFixEnabled;
                animationDyes = d.animationDyes;
                animationSkins = d.animationSkins;
            }
            case "qol.disconnect_fix" -> disconnectFixEnabled = d.disconnectFixEnabled;
            case "qol.double_use_fix" -> doubleUseFixEnabled = d.doubleUseFixEnabled;
            case "qol.eye_height_fix" -> eyeHeightFixEnabled = d.eyeHeightFixEnabled;
            case "qol.instant_sneak" -> instantSneakEnabled = d.instantSneakEnabled;
            case "qol.item_count_fix" -> itemCountFixEnabled = d.itemCountFixEnabled;
            case "qol.item_scale" -> {
                itemScaleEnabled = d.itemScaleEnabled;
                itemScale = d.itemScale;
            }
            case "qol.active_pet_highlight" -> {
                activePetHighlightEnabled = d.activePetHighlightEnabled;
                activePetHighlightColor = d.activePetHighlightColor;
            }
            case "qol.anvil_helper" -> {
                anvilHelperEnabled = d.anvilHelperEnabled;
                anvilHelperColor = d.anvilHelperColor;
            }
            case "qol.calendar_date" -> {
                calendarDateEnabled = d.calendarDateEnabled;
                calendarMinister = d.calendarMinister;
            }
            case "qol.experiment_solver" -> {
                experimentSolverEnabled = d.experimentSolverEnabled;
                experimentChronomatron = d.experimentChronomatron;
                experimentUltrasequencer = d.experimentUltrasequencer;
                experimentSuperpairs = d.experimentSuperpairs;
                experimentBlockWrongClicks = d.experimentBlockWrongClicks;
                experimentHideTooltip = d.experimentHideTooltip;
                experimentHideWrongChrono = d.experimentHideWrongChrono;
                experimentHideWrongUltra = d.experimentHideWrongUltra;
                experimentPrivateIslandOnly = d.experimentPrivateIslandOnly;
                experimentFirstColor = d.experimentFirstColor;
                experimentSecondColor = d.experimentSecondColor;
                experimentMatchedColor = d.experimentMatchedColor;
                experimentMatchColor = d.experimentMatchColor;
                experimentPowerupColor = d.experimentPowerupColor;
            }
            case "qol.auto_experiments" -> {
                autoExperimentsEnabled = d.autoExperimentsEnabled;
                autoExperimentsClickDelay = d.autoExperimentsClickDelay;
                autoExperimentsDelayVariety = d.autoExperimentsDelayVariety;
                autoExperimentsAutoClose = d.autoExperimentsAutoClose;
                autoExperimentsSerumCount = d.autoExperimentsSerumCount;
                autoExperimentsGetMaxXp = d.autoExperimentsGetMaxXp;
            }
            case "qol.cheater_wardrobe" -> {
                cheaterWardrobeEnabled = d.cheaterWardrobeEnabled;
                cheaterWardrobeMoveEquip = d.cheaterWardrobeMoveEquip;
                cheaterWardrobeStationaryOnly = d.cheaterWardrobeStationaryOnly;
                cheaterWardrobeResetOpen = d.cheaterWardrobeResetOpen;
                cheaterWardrobeClickDelay = d.cheaterWardrobeClickDelay;
                cheaterWardrobeCloseDelay = d.cheaterWardrobeCloseDelay;
                cheaterWardrobeDelayVariance = d.cheaterWardrobeDelayVariance;
                cheaterWardrobeSlot1 = d.cheaterWardrobeSlot1;
                cheaterWardrobeSlot2 = d.cheaterWardrobeSlot2;
                cheaterWardrobeSlot3 = d.cheaterWardrobeSlot3;
                cheaterWardrobeSlot4 = d.cheaterWardrobeSlot4;
                cheaterWardrobeSlot5 = d.cheaterWardrobeSlot5;
                cheaterWardrobeSlot6 = d.cheaterWardrobeSlot6;
                cheaterWardrobeSlot7 = d.cheaterWardrobeSlot7;
                cheaterWardrobeSlot8 = d.cheaterWardrobeSlot8;
                cheaterWardrobeSlot9 = d.cheaterWardrobeSlot9;
            }
            case "qol.escrow_fix" -> escrowFixEnabled = d.escrowFixEnabled;
            case "qol.auto_harp" -> autoHarpEnabled = d.autoHarpEnabled;
            case "qol.auto_gfs" -> {
                autoGfsEnabled = d.autoGfsEnabled;
                autoGfsInSkyblock = d.autoGfsInSkyblock;
                autoGfsInKuudra = d.autoGfsInKuudra;
                autoGfsInDungeon = d.autoGfsInDungeon;
                autoGfsRefillOnDungeonStart = d.autoGfsRefillOnDungeonStart;
                autoGfsRefillOnTimer = d.autoGfsRefillOnTimer;
                autoGfsTimerIncrements = d.autoGfsTimerIncrements;
                autoGfsRefillPearl = d.autoGfsRefillPearl;
                autoGfsRefillJerry = d.autoGfsRefillJerry;
                autoGfsRefillTnt = d.autoGfsRefillTnt;
                autoGfsRefillLeap = d.autoGfsRefillLeap;
                autoGfsRefillTwilight = d.autoGfsRefillTwilight;
                autoGfsAutoGetDraft = d.autoGfsAutoGetDraft;
                autoGfsKeybind = d.autoGfsKeybind;
            }
            case "qol.auto_sell" -> {
                autoSellEnabled = d.autoSellEnabled;
                autoSellDelay = d.autoSellDelay;
                autoSellRandomization = d.autoSellRandomization;
                autoSellClickType = d.autoSellClickType;
                autoSellItems = d.autoSellItems == null
                        ? new java.util.ArrayList<>()
                        : new java.util.ArrayList<>(d.autoSellItems);
                autoSellKeybind = d.autoSellKeybind;
            }
            case "qol.ghosts" -> {
                ghostsEnabled = d.ghostsEnabled;
                ghostsShowGhosts = d.ghostsShowGhosts;
                ghostsShowPowered = d.ghostsShowPowered;
                ghostsHighlightStyle = d.ghostsHighlightStyle;
                ghostsFillColor = d.ghostsFillColor;
                ghostsOutlineColor = d.ghostsOutlineColor;
                ghostsKeybind = d.ghostsKeybind;
            }
            case "qol.auto_dojo" -> {
                autoDojoEnabled = d.autoDojoEnabled;
                autoDojoControl = d.autoDojoControl;
                autoDojoMastery = d.autoDojoMastery;
                autoDojoDiscipline = d.autoDojoDiscipline;
                autoDojoDisciplineAttack = d.autoDojoDisciplineAttack;
                autoDojoControlPredict = d.autoDojoControlPredict;
                autoDojoMasteryDelay = d.autoDojoMasteryDelay;
            }
            case "qol.fishing_creatures" -> {
                fishingCreaturesEnabled = d.fishingCreaturesEnabled;
                fishingCreaturesHud = d.fishingCreaturesHud;
                fishingCreaturesCapNotify = d.fishingCreaturesCapNotify;
                fishingCreaturesTimerNotify = d.fishingCreaturesTimerNotify;
                fishingCreaturesTimerLength = d.fishingCreaturesTimerLength;
                fishingCreaturesMinRarity = d.fishingCreaturesMinRarity;
                fishingCreaturesRareAnnounce = d.fishingCreaturesRareAnnounce;
                fishingCreaturesRareSound = d.fishingCreaturesRareSound;
                fishingCreaturesRareParty = d.fishingCreaturesRareParty;
                fishingCreaturesRareEsp = d.fishingCreaturesRareEsp;
                fishingCreaturesEspColor = d.fishingCreaturesEspColor;
                fishingCreaturesShortenChat = d.fishingCreaturesShortenChat;
                fishingCreaturesHideCommon = d.fishingCreaturesHideCommon;
                fishingCreaturesAutoAttack = d.fishingCreaturesAutoAttack;
                fishingCreaturesAutoDelay = d.fishingCreaturesAutoDelay;
                fishingCreaturesThunderSparks = d.fishingCreaturesThunderSparks;
            }
            case "qol.fishing_hotspots" -> {
                fishingHotspotsEnabled = d.fishingHotspotsEnabled;
                fishingHotspotsCircle = d.fishingHotspotsCircle;
                fishingHotspotsHideParticles = d.fishingHotspotsHideParticles;
                fishingHotspotsRadar = d.fishingHotspotsRadar;
                fishingHotspotsTracer = d.fishingHotspotsTracer;
                fishingHotspotsDespawn = d.fishingHotspotsDespawn;
                fishingHotspotsColor = d.fishingHotspotsColor;
            }
            case "qol.fishing_trophy" -> {
                fishingTrophyEnabled = d.fishingTrophyEnabled;
                fishingTrophyTitles = d.fishingTrophyTitles;
                fishingTrophyFilterChat = d.fishingTrophyFilterChat;
                fishingTrophyMinRarity = d.fishingTrophyMinRarity;
                fishingTrophyGoldenTimer = d.fishingTrophyGoldenTimer;
                fishingTrophyGeyser = d.fishingTrophyGeyser;
                fishingTrophySponge = d.fishingTrophySponge;
                fishingTrophyFillet = d.fishingTrophyFillet;
            }
            case "qol.fishing_visuals" -> {
                fishingVisualsEnabled = d.fishingVisualsEnabled;
                fishingVisualsHideOtherBobbers = d.fishingVisualsHideOtherBobbers;
                fishingVisualsChumHider = d.fishingVisualsChumHider;
                fishingVisualsMuteBanshee = d.fishingVisualsMuteBanshee;
                fishingVisualsMuteDrake = d.fishingVisualsMuteDrake;
            }
            case "qol.fishing_tools" -> {
                fishingToolsEnabled = d.fishingToolsEnabled;
                fishingToolsBaitHud = d.fishingToolsBaitHud;
                fishingToolsNoBaitWarn = d.fishingToolsNoBaitWarn;
                fishingToolsBaitChange = d.fishingToolsBaitChange;
                fishingToolsThunderNotify = d.fishingToolsThunderNotify;
                fishingToolsTotemHud = d.fishingToolsTotemHud;
            }
            case "qol.mining_scatha" -> {
                miningScathaEnabled = d.miningScathaEnabled;
                miningScathaTitles = d.miningScathaTitles;
                miningScathaSounds = d.miningScathaSounds;
                miningScathaCooldown = d.miningScathaCooldown;
                miningScathaPetDrop = d.miningScathaPetDrop;
                miningScathaPetRarity = d.miningScathaPetRarity;
                miningScathaHud = d.miningScathaHud;
                miningScathaParty = d.miningScathaParty;
            }
            case "qol.mining_events" -> {
                miningEventsEnabled = d.miningEventsEnabled;
                miningEventsHud = d.miningEventsHud;
                miningEventsTitles = d.miningEventsTitles;
                miningEventsGoblinEsp = d.miningEventsGoblinEsp;
            }
            case "qol.mining_glacite" -> {
                miningGlaciteEnabled = d.miningGlaciteEnabled;
                miningGlacitePityHud = d.miningGlacitePityHud;
                miningGlaciteCorpseHud = d.miningGlaciteCorpseHud;
                miningGlaciteColdOverlay = d.miningGlaciteColdOverlay;
                miningGlacitePartyShare = d.miningGlacitePartyShare;
                miningGlaciteShaftParty = d.miningGlaciteShaftParty;
                miningGlacitePityChat = d.miningGlacitePityChat;
                miningGlaciteEnterTitle = d.miningGlaciteEnterTitle;
                miningGlaciteEnterChat = d.miningGlaciteEnterChat;
                miningGlaciteCorpseWaypoints = d.miningGlaciteCorpseWaypoints;
                miningGlaciteKeyAnnounce = d.miningGlaciteKeyAnnounce;
                miningGlaciteEnterParty = d.miningGlaciteEnterParty;
            }
            case "qol.mining_helpers" -> {
                miningHelpersEnabled = d.miningHelpersEnabled;
                miningHelpersFetchur = d.miningHelpersFetchur;
                miningHelpersFossilMuncher = d.miningHelpersFossilMuncher;
                miningHelpersDrillFuel = d.miningHelpersDrillFuel;
                miningHelpersAbilityHud = d.miningHelpersAbilityHud;
                miningHelpersCommissionGui = d.miningHelpersCommissionGui;
                miningHelpersCommissionMobs = d.miningHelpersCommissionMobs;
                miningHelpersNotifyPortal = d.miningHelpersNotifyPortal;
                miningHelpersNotifyScrap = d.miningHelpersNotifyScrap;
                miningHelpersNotifyGoblin = d.miningHelpersNotifyGoblin;
                miningHelpersMetalDistance = d.miningHelpersMetalDistance;
                miningHelpersDetectorSolver = d.miningHelpersDetectorSolver;
                miningHelpersDetectorDing = d.miningHelpersDetectorDing;
                miningHelpersDetectorTitle = d.miningHelpersDetectorTitle;
                miningHelpersRedCarpets = d.miningHelpersRedCarpets;
                miningHelpersFossilExcavator = d.miningHelpersFossilExcavator;
                miningHelpersWishingCompass = d.miningHelpersWishingCompass;
                miningHelpersCallKing = d.miningHelpersCallKing;
                miningHelpersBreakReset = d.miningHelpersBreakReset;
                miningHelpersGemstoneDesync = d.miningHelpersGemstoneDesync;
            }
            case "qol.mining_hotm" -> {
                miningHotmEnabled = d.miningHotmEnabled;
                miningHotmSkyMall = d.miningHotmSkyMall;
                miningHotmScreenHint = d.miningHotmScreenHint;
                miningHudX = d.miningHudX;
                miningHudY = d.miningHudY;
            }
            case "qol.diana_burrows" -> {
                dianaBurrowsEnabled = d.dianaBurrowsEnabled;
                dianaBurrowsGuess = d.dianaBurrowsGuess;
                dianaBurrowsParticles = d.dianaBurrowsParticles;
                dianaBurrowsWaypoints = d.dianaBurrowsWaypoints;
                dianaBurrowsMuteSpade = d.dianaBurrowsMuteSpade;
                dianaBurrowsFixChat = d.dianaBurrowsFixChat;
                dianaBurrowsGuessColor = d.dianaBurrowsGuessColor;
                dianaBurrowsStartColor = d.dianaBurrowsStartColor;
                dianaBurrowsMobColor = d.dianaBurrowsMobColor;
                dianaBurrowsTreasureColor = d.dianaBurrowsTreasureColor;
                dianaHudX = d.dianaHudX;
                dianaHudY = d.dianaHudY;
            }
            case "qol.diana_mobs" -> {
                dianaMobsEnabled = d.dianaMobsEnabled;
                dianaMobsRareEsp = d.dianaMobsRareEsp;
                dianaMobsGriffinWarn = d.dianaMobsGriffinWarn;
                dianaMobsEspColor = d.dianaMobsEspColor;
            }
            case "qol.diana_profit" -> {
                dianaProfitEnabled = d.dianaProfitEnabled;
                dianaProfitHud = d.dianaProfitHud;
                dianaHudX = d.dianaHudX;
                dianaHudY = d.dianaHudY;
            }
            case "qol.diana_share" -> {
                dianaShareEnabled = d.dianaShareEnabled;
                dianaShareParty = d.dianaShareParty;
                dianaShareAutoWarp = d.dianaShareAutoWarp;
            }
            case "qol.foraging_trees" -> {
                foragingTreesEnabled = d.foragingTreesEnabled;
                foragingTreesProgressHud = d.foragingTreesProgressHud;
                foragingTreesOnlyAxe = d.foragingTreesOnlyAxe;
                foragingTreesHideBits = d.foragingTreesHideBits;
                foragingTreesGiftHud = d.foragingTreesGiftHud;
                foragingTreesHideUnmineable = d.foragingTreesHideUnmineable;
                foragingTreesFellTitle = d.foragingTreesFellTitle;
            }
            case "qol.foraging_audio" -> {
                foragingAudioEnabled = d.foragingAudioEnabled;
                foragingAudioMutePhantom = d.foragingAudioMutePhantom;
                foragingAudioMuteTreeBreak = d.foragingAudioMuteTreeBreak;
                foragingAudioMuteBreakGalatea = d.foragingAudioMuteBreakGalatea;
                foragingAudioMuteFusion = d.foragingAudioMuteFusion;
                foragingAudioMuteStereo = d.foragingAudioMuteStereo;
            }
            case "qol.foraging_helpers" -> {
                foragingHelpersEnabled = d.foragingHelpersEnabled;
                foragingHelpersSweepHud = d.foragingHelpersSweepHud;
                foragingHelpersTempleSolver = d.foragingHelpersTempleSolver;
                foragingHelpersBeaconHints = d.foragingHelpersBeaconHints;
                foragingHelpersHighlights = d.foragingHelpersHighlights;
                foragingHelpersMoongladeBeacon = d.foragingHelpersMoongladeBeacon;
                foragingHelpersParkTutorial = d.foragingHelpersParkTutorial;
                foragingHelpersHuntingEsp = d.foragingHelpersHuntingEsp;
                foragingHelpersFrogMask = d.foragingHelpersFrogMask;
                foragingHelpersLassoHud = d.foragingHelpersLassoHud;
                foragingHelpersCinderbat = d.foragingHelpersCinderbat;
                foragingHelpersHuntaxeLock = d.foragingHelpersHuntaxeLock;
                foragingHelpersShardTracker = d.foragingHelpersShardTracker;
                foragingHelpersLassoAlert = d.foragingHelpersLassoAlert;
                foragingHelpersSeaLumiesMin = d.foragingHelpersSeaLumiesMin;
                foragingHelpersHotfHint = d.foragingHelpersHotfHint;
                foragingHudX = d.foragingHudX;
                foragingHudY = d.foragingHudY;
            }
            case "qol.foraging_cheats" -> {
                foragingCheatsEnabled = d.foragingCheatsEnabled;
                foragingCheatsAutoBeacon = d.foragingCheatsAutoBeacon;
                foragingCheatsAutoChop = d.foragingCheatsAutoChop;
                foragingCheatsAxeToss = d.foragingCheatsAxeToss;
                foragingCheatsMinCluster = d.foragingCheatsMinCluster;
                foragingCheatsClickDelay = d.foragingCheatsClickDelay;
            }
            case "qol.dungeon_hud" -> {
                dungeonHudEnabled = d.dungeonHudEnabled;
                dungeonHudSecrets = d.dungeonHudSecrets;
                dungeonHudScore = d.dungeonHudScore;
                dungeonHudClass = d.dungeonHudClass;
                dungeonHudFloor = d.dungeonHudFloor;
                dungeonHudCleared = d.dungeonHudCleared;
                dungeonHudInvincibility = d.dungeonHudInvincibility;
                dungeonHudMaskOverlay = d.dungeonHudMaskOverlay;
                dungeonHudMaskOverlayColor = d.dungeonHudMaskOverlayColor;
                dungeonHudTerracotta = d.dungeonHudTerracotta;
                dungeonHudBlessings = d.dungeonHudBlessings;
                dungeonHudF7Timers = d.dungeonHudF7Timers;
                dungeonHudRagnarock = d.dungeonHudRagnarock;
                dungeonHudMelody = d.dungeonHudMelody;
                dungeonHudMelodyOther = d.dungeonHudMelodyOther;
                dungeonHudQuiz = d.dungeonHudQuiz;
                dungeonHudMap = d.dungeonHudMap;
                dungeonHudMapMode = d.dungeonHudMapMode;
                dungeonHudMapDoors = d.dungeonHudMapDoors;
                dungeonHudMapPlayers = d.dungeonHudMapPlayers;
                dungeonHudMapExtra = d.dungeonHudMapExtra;
                dungeonHudMapHideBoss = d.dungeonHudMapHideBoss;
                dungeonHudCrypts = d.dungeonHudCrypts;
                dungeonHudDeaths = d.dungeonHudDeaths;
                dungeonHudScoreOverlay = d.dungeonHudScoreOverlay;
                dungeonHudClassIcons = d.dungeonHudClassIcons;
                dungeonHudHeadMarkers = d.dungeonHudHeadMarkers;
                dungeonHudRoomNames = d.dungeonHudRoomNames;
                dungeonHudRoomSecrets = d.dungeonHudRoomSecrets;
                dungeonHudPlayerNames = d.dungeonHudPlayerNames;
                dungeonHudMapMimic = d.dungeonHudMapMimic;
                dungeonHudMapPuzzles = d.dungeonHudMapPuzzles;
                dungeonHudMapScale = d.dungeonHudMapScale;
                dungeonHudPuzzleTimer = d.dungeonHudPuzzleTimer;
                dungeonHudWarpCooldown = d.dungeonHudWarpCooldown;
                dungeonHudSecretSpawn = d.dungeonHudSecretSpawn;
                dungeonHudExplosiveShot = d.dungeonHudExplosiveShot;
                dungeonHudUnclaimedChests = d.dungeonHudUnclaimedChests;
                dungeonHudChestWarning = d.dungeonHudChestWarning;
                dungeonHudChestWarningCount = d.dungeonHudChestWarningCount;
                dungeonHudExtraStats = d.dungeonHudExtraStats;
                dungeonHudLedge = d.dungeonHudLedge;
                dungeonHudLedgeYellow = d.dungeonHudLedgeYellow;
                dungeonHudLedgeAll = d.dungeonHudLedgeAll;
                dungeonHudRunTimers = d.dungeonHudRunTimers;
                dungeonHudShowSplitPbs = d.dungeonHudShowSplitPbs;
                dungeonSplitPbs = d.dungeonSplitPbs;
                dungeonHudKuudraSplits = d.dungeonHudKuudraSplits;
                kuudraSplitPbs = d.kuudraSplitPbs;
                dungeonHudCheaterMap = d.dungeonHudCheaterMap;
                dungeonHudCheaterNames = d.dungeonHudCheaterNames;
                dungeonHudCheaterDarken = d.dungeonHudCheaterDarken;
                dungeonHudCheaterDarkenFactor = d.dungeonHudCheaterDarkenFactor;
            }
            case "qol.dungeon_esp" -> {
                dungeonEspEnabled = d.dungeonEspEnabled;
                dungeonEspStarred = d.dungeonEspStarred;
                dungeonEspTeammates = d.dungeonEspTeammates;
                dungeonEspBats = d.dungeonEspBats;
                dungeonEspFels = d.dungeonEspFels;
                dungeonEspShadow = d.dungeonEspShadow;
                dungeonEspKeys = d.dungeonEspKeys;
                dungeonEspMimic = d.dungeonEspMimic;
                dungeonEspTracers = d.dungeonEspTracers;
                dungeonEspDepth = d.dungeonEspDepth;
                dungeonEspGhostBlock = d.dungeonEspGhostBlock;
                dungeonEspGhostUayor = d.dungeonEspGhostUayor;
                dungeonEspGhostStonk = d.dungeonEspGhostStonk;
                dungeonEspGhostKeybind = d.dungeonEspGhostKeybind;
                dungeonEspTriggerBot = d.dungeonEspTriggerBot;
                dungeonEspTriggerCrystal = d.dungeonEspTriggerCrystal;
                dungeonEspTriggerTake = d.dungeonEspTriggerTake;
                dungeonEspTriggerPlace = d.dungeonEspTriggerPlace;
                dungeonEspTriggerSecret = d.dungeonEspTriggerSecret;
                dungeonEspTriggerDelay = d.dungeonEspTriggerDelay;
                dungeonEspFill = d.dungeonEspFill;
                dungeonEspOpacity = d.dungeonEspOpacity;
                dungeonEspStarredColor = d.dungeonEspStarredColor;
                dungeonEspBatColor = d.dungeonEspBatColor;
                dungeonEspFelColor = d.dungeonEspFelColor;
                dungeonEspKeyColor = d.dungeonEspKeyColor;
                dungeonEspBloodKeyColor = d.dungeonEspBloodKeyColor;
                dungeonEspShadowColor = d.dungeonEspShadowColor;
                dungeonEspTeammateColor = d.dungeonEspTeammateColor;
                dungeonEspMimicColor = d.dungeonEspMimicColor;
                dungeonEspWither = d.dungeonEspWither;
                dungeonEspCrystals = d.dungeonEspCrystals;
                dungeonEspSecrets = d.dungeonEspSecrets;
                dungeonEspSecretWaypoints = d.dungeonEspSecretWaypoints;
                dungeonEspHideCollected = d.dungeonEspHideCollected;
                dungeonEspSecretClicked = d.dungeonEspSecretClicked;
                dungeonEspSecretClickedColor = d.dungeonEspSecretClickedColor;
                dungeonEspSecretClickedLockedColor = d.dungeonEspSecretClickedLockedColor;
                dungeonEspSecretClickedSeconds = d.dungeonEspSecretClickedSeconds;
                dungeonEspSecretClickedBoss = d.dungeonEspSecretClickedBoss;
                dungeonEspItems = d.dungeonEspItems;
                dungeonEspIcedMobs = d.dungeonEspIcedMobs;
                dungeonEspSimon = d.dungeonEspSimon;
                dungeonEspWitherColor = d.dungeonEspWitherColor;
                dungeonEspCrystalColor = d.dungeonEspCrystalColor;
                dungeonEspSecretColor = d.dungeonEspSecretColor;
                dungeonEspChestColor = d.dungeonEspChestColor;
                dungeonEspSimonColor = d.dungeonEspSimonColor;
                dungeonEspHateDoors = d.dungeonEspHateDoors;
                dungeonEspHateWither = d.dungeonEspHateWither;
                dungeonEspHateBlood = d.dungeonEspHateBlood;
                dungeonEspHateEntrance = d.dungeonEspHateEntrance;
                dungeonEspHateWitherGlass = d.dungeonEspHateWitherGlass;
                dungeonEspHateBloodGlass = d.dungeonEspHateBloodGlass;
                dungeonEspHateEntranceGlass = d.dungeonEspHateEntranceGlass;
                dungeonEspLivid = d.dungeonEspLivid;
                dungeonEspThorn = d.dungeonEspThorn;
                dungeonEspSpiritBear = d.dungeonEspSpiritBear;
                dungeonEspDoors = d.dungeonEspDoors;
                dungeonEspBloodBox = d.dungeonEspBloodBox;
                dungeonEspLividColor = d.dungeonEspLividColor;
                dungeonEspThornColor = d.dungeonEspThornColor;
                dungeonEspSpiritBearColor = d.dungeonEspSpiritBearColor;
                dungeonEspBloodBoxColor = d.dungeonEspBloodBoxColor;
                dungeonEspBloodLineColor = d.dungeonEspBloodLineColor;
            }
            case "qol.dungeon_announce" -> {
                dungeonAnnounceEnabled = d.dungeonAnnounceEnabled;
                dungeonAnnounceMimic = d.dungeonAnnounceMimic;
                dungeonAnnouncePrince = d.dungeonAnnouncePrince;
                dungeonAnnounceBat = d.dungeonAnnounceBat;
                dungeonAnnounceBlood = d.dungeonAnnounceBlood;
                dungeonAnnounceScoreTitle = d.dungeonAnnounceScoreTitle;
                dungeonAnnounceScoreThreshold = d.dungeonAnnounceScoreThreshold;
                dungeonAnnounceF7 = d.dungeonAnnounceF7;
                dungeonAnnounceRagnarock = d.dungeonAnnounceRagnarock;
                dungeonAnnounceRooms = d.dungeonAnnounceRooms;
                dungeonAnnounceMelody = d.dungeonAnnounceMelody;
                dungeonAnnounceMelodyParty = d.dungeonAnnounceMelodyParty;
                dungeonAnnounceMelodyMessage = d.dungeonAnnounceMelodyMessage;
                dungeonAnnounceMelodyProgress = d.dungeonAnnounceMelodyProgress;
                dungeonAnnounceDeath = d.dungeonAnnounceDeath;
                dungeonAnnounceDeathMessage = d.dungeonAnnounceDeathMessage;
                dungeonAnnouncePosition = d.dungeonAnnouncePosition;
                dungeonAnnounceSecretChime = d.dungeonAnnounceSecretChime;
                dungeonAnnounceDuplicateClass = d.dungeonAnnounceDuplicateClass;
                dungeonAnnouncePlayerCount = d.dungeonAnnouncePlayerCount;
                dungeonAnnounceLocation = d.dungeonAnnounceLocation;
                dungeonAnnounceKeyDrop = d.dungeonAnnounceKeyDrop;
                dungeonAnnounceKeyDropAll = d.dungeonAnnounceKeyDropAll;
                dungeonAnnounceAutoUlt = d.dungeonAnnounceAutoUlt;
            }
            case "qol.dungeon_leap" -> {
                dungeonLeapEnabled = d.dungeonLeapEnabled;
                dungeonLeapHighlight = d.dungeonLeapHighlight;
                dungeonLeapCustomGui = d.dungeonLeapCustomGui;
                dungeonLeapAnnounce = d.dungeonLeapAnnounce;
                dungeonLeapCounter = d.dungeonLeapCounter;
                dungeonLeapMessage = d.dungeonLeapMessage;
                dungeonLeapKeys = d.dungeonLeapKeys;
            }
            case "qol.dungeon_terminals" -> {
                dungeonTerminalsEnabled = d.dungeonTerminalsEnabled;
                dungeonTerminalsOverlay = d.dungeonTerminalsOverlay;
                dungeonTerminalsAuto = d.dungeonTerminalsAuto;
                dungeonTerminalsMelody = d.dungeonTerminalsMelody;
                dungeonTerminalsNumbers = d.dungeonTerminalsNumbers;
                dungeonTerminalsNumbersShow = d.dungeonTerminalsNumbersShow;
                dungeonTerminalsRubix = d.dungeonTerminalsRubix;
                dungeonTerminalsColors = d.dungeonTerminalsColors;
                dungeonTerminalsPanes = d.dungeonTerminalsPanes;
                dungeonTerminalsStarts = d.dungeonTerminalsStarts;
                dungeonTerminalsAutoMelody = d.dungeonTerminalsAutoMelody;
                dungeonTerminalsMelodySkip = d.dungeonTerminalsMelodySkip;
                dungeonTerminalsMelodySkipFirstRow = d.dungeonTerminalsMelodySkipFirstRow;
                dungeonTerminalsMelodySkipMode = d.dungeonTerminalsMelodySkipMode;
                dungeonTerminalsSounds = d.dungeonTerminalsSounds;
                dungeonTerminalsCompleteSounds = d.dungeonTerminalsCompleteSounds;
                dungeonTerminalsStopTooltips = d.dungeonTerminalsStopTooltips;
                dungeonTerminalsHideClicked = d.dungeonTerminalsHideClicked;
                dungeonTerminalsBlockWrongSlots = d.dungeonTerminalsBlockWrongSlots;
                dungeonTerminalsHumanOrder = d.dungeonTerminalsHumanOrder;
                dungeonTerminalsMelodyKeys = d.dungeonTerminalsMelodyKeys;
                dungeonTerminalsProtect = d.dungeonTerminalsProtect;
                dungeonTerminalsProtectMs = d.dungeonTerminalsProtectMs;
                dungeonTerminalsAutoNumbers = d.dungeonTerminalsAutoNumbers;
                dungeonTerminalsAutoColors = d.dungeonTerminalsAutoColors;
                dungeonTerminalsAutoRubix = d.dungeonTerminalsAutoRubix;
                dungeonTerminalsAutoPanes = d.dungeonTerminalsAutoPanes;
                dungeonTerminalsAutoStarts = d.dungeonTerminalsAutoStarts;
                dungeonTerminalsQueue = d.dungeonTerminalsQueue;
                dungeonTerminalsClone = d.dungeonTerminalsClone;
                dungeonTerminalsDelay = d.dungeonTerminalsDelay;
                dungeonTerminalsColor = d.dungeonTerminalsColor;
                dungeonTerminalsHitboxes = d.dungeonTerminalsHitboxes;
                dungeonTerminalsHitboxColor = d.dungeonTerminalsHitboxColor;
                dungeonTerminalsNumbers1Color = d.dungeonTerminalsNumbers1Color;
                dungeonTerminalsNumbers2Color = d.dungeonTerminalsNumbers2Color;
                dungeonTerminalsNumbers3Color = d.dungeonTerminalsNumbers3Color;
                dungeonTerminalsRubixPosColor = d.dungeonTerminalsRubixPosColor;
                dungeonTerminalsRubixNegColor = d.dungeonTerminalsRubixNegColor;
                dungeonTerminalsMelodyColumnColor = d.dungeonTerminalsMelodyColumnColor;
                dungeonTerminalsMelodyIndicatorColor = d.dungeonTerminalsMelodyIndicatorColor;
                dungeonTerminalsMelodyWrongColor = d.dungeonTerminalsMelodyWrongColor;
                athen().copyModule(moduleId, d.athen());
            }
            case "qol.dungeon_termsim" -> {
                dungeonTermSimEnabled = d.dungeonTermSimEnabled;
                dungeonTermSimKeybind = d.dungeonTermSimKeybind;
                dungeonTermSimPing = d.dungeonTermSimPing;
                dungeonTermSimShowPbs = d.dungeonTermSimShowPbs;
                dungeonTermSimPbs = d.dungeonTermSimPbs;
                athen().copyModule(moduleId, d.athen());
            }
            case "qol.dungeon_requeue" -> {
                dungeonRequeueEnabled = d.dungeonRequeueEnabled;
                dungeonRequeueDelay = d.dungeonRequeueDelay;
            }
            case "qol.dungeon_puzzles" -> {
                dungeonPuzzlesEnabled = d.dungeonPuzzlesEnabled;
                dungeonPuzzlesQuiz = d.dungeonPuzzlesQuiz;
                dungeonPuzzlesQuizBoxes = d.dungeonPuzzlesQuizBoxes;
                dungeonPuzzlesQuizTimer = d.dungeonPuzzlesQuizTimer;
                dungeonPuzzlesWeirdos = d.dungeonPuzzlesWeirdos;
                dungeonPuzzlesBlaze = d.dungeonPuzzlesBlaze;
                dungeonPuzzlesIce = d.dungeonPuzzlesIce;
                dungeonPuzzlesIcePath = d.dungeonPuzzlesIcePath;
                dungeonPuzzlesIceOptimize = d.dungeonPuzzlesIceOptimize;
                dungeonPuzzlesWater = d.dungeonPuzzlesWater;
                dungeonPuzzlesWaterOptimized = d.dungeonPuzzlesWaterOptimized;
                dungeonPuzzlesBoulder = d.dungeonPuzzlesBoulder;
                dungeonPuzzlesTpMaze = d.dungeonPuzzlesTpMaze;
                dungeonPuzzlesCreeperBeams = d.dungeonPuzzlesCreeperBeams;
                dungeonPuzzlesTicTacToe = d.dungeonPuzzlesTicTacToe;
            }
            case "qol.dungeon_f7" -> {
                dungeonF7Enabled = d.dungeonF7Enabled;
                dungeonF7Titles = d.dungeonF7Titles;
                dungeonF7TitleCrystal = d.dungeonF7TitleCrystal;
                dungeonF7TitleWither = d.dungeonF7TitleWither;
                dungeonF7TitleTerminal = d.dungeonF7TitleTerminal;
                dungeonF7TitleGate = d.dungeonF7TitleGate;
                dungeonF7HideOtherTitles = d.dungeonF7HideOtherTitles;
                dungeonF7HideTitlesAtSs = d.dungeonF7HideTitlesAtSs;
                dungeonF7HideTitlesAtPre4 = d.dungeonF7HideTitlesAtPre4;
                dungeonF7HideAtSs = d.dungeonF7HideAtSs;
                dungeonF7HideAtSsPreTerms = d.dungeonF7HideAtSsPreTerms;
                dungeonF7HideAfterLeap = d.dungeonF7HideAfterLeap;
                dungeonF7HideAfterLeapBoss = d.dungeonF7HideAfterLeapBoss;
                dungeonF7TitleCrystalText = d.dungeonF7TitleCrystalText;
                dungeonF7TitleWitherText = d.dungeonF7TitleWitherText;
                dungeonF7TitleTerminalText = d.dungeonF7TitleTerminalText;
                dungeonF7TitleGateText = d.dungeonF7TitleGateText;
                dungeonF7Timers = d.dungeonF7Timers;
                dungeonF7TimerTicks = d.dungeonF7TimerTicks;
                dungeonF7TimerSymbol = d.dungeonF7TimerSymbol;
                dungeonF7TimerPrefix = d.dungeonF7TimerPrefix;
                dungeonF7TimerMaxor = d.dungeonF7TimerMaxor;
                dungeonF7TimerStorm = d.dungeonF7TimerStorm;
                dungeonF7TimerPad = d.dungeonF7TimerPad;
                dungeonF7TimerLightning = d.dungeonF7TimerLightning;
                dungeonF7TimerGoldor = d.dungeonF7TimerGoldor;
                dungeonF7TimerNecron = d.dungeonF7TimerNecron;
                dungeonF7Crystals = d.dungeonF7Crystals;
                dungeonF7MaxorStun = d.dungeonF7MaxorStun;
                dungeonF7StormCrush = d.dungeonF7StormCrush;
                dungeonF7StormLb = d.dungeonF7StormLb;
                dungeonF7TermStart = d.dungeonF7TermStart;
                dungeonF7CrystalSpawn = d.dungeonF7CrystalSpawn;
                dungeonF7CrystalPlace = d.dungeonF7CrystalPlace;
                dungeonF7CrystalAlert = d.dungeonF7CrystalAlert;
                dungeonF7MelodyDisplay = d.dungeonF7MelodyDisplay;
                dungeonF7WitherEsp = d.dungeonF7WitherEsp;
                dungeonF7MaxorColor = d.dungeonF7MaxorColor;
                dungeonF7StormColor = d.dungeonF7StormColor;
                dungeonF7GoldorColor = d.dungeonF7GoldorColor;
                dungeonF7NecronColor = d.dungeonF7NecronColor;
                dungeonF7Dragons = d.dungeonF7Dragons;
                dungeonF7DragonSpray = d.dungeonF7DragonSpray;
                dungeonF7DragonArrows = d.dungeonF7DragonArrows;
                dungeonF7DragonHealth = d.dungeonF7DragonHealth;
                dungeonF7DragonBoxes = d.dungeonF7DragonBoxes;
                dungeonF7DragonTracers = d.dungeonF7DragonTracers;
                dungeonF7DragonPriority = d.dungeonF7DragonPriority;
                dungeonF7DragonPaul = d.dungeonF7DragonPaul;
                dungeonF7DragonSoloClass = d.dungeonF7DragonSoloClass;
                dungeonF7P3Display = d.dungeonF7P3Display;
                dungeonF7SharpShooter = d.dungeonF7SharpShooter;
                dungeonF7SharpAim = d.dungeonF7SharpAim;
                dungeonF7SharpComplete = d.dungeonF7SharpComplete;
                dungeonF7SharpMarkedColor = d.dungeonF7SharpMarkedColor;
                dungeonF7SharpTargetColor = d.dungeonF7SharpTargetColor;
                dungeonF7SharpAim1Color = d.dungeonF7SharpAim1Color;
                dungeonF7SharpAim2Color = d.dungeonF7SharpAim2Color;
                dungeonF7SharpAim3Color = d.dungeonF7SharpAim3Color;
                dungeonF7TermTimes = d.dungeonF7TermTimes;
                dungeonF7TermPbs = d.dungeonF7TermPbs;
                dungeonF7TermPbTimes = d.dungeonF7TermPbTimes;
                dungeonF7Predev = d.dungeonF7Predev;
                dungeonF7PredevAll = d.dungeonF7PredevAll;
                dungeonF7PredevPbMs = d.dungeonF7PredevPbMs;
                dungeonF7SsComplete = d.dungeonF7SsComplete;
                dungeonF7Pre4Complete = d.dungeonF7Pre4Complete;
                dungeonF7DragonTimer = d.dungeonF7DragonTimer;
                dungeonF7GoldorFrenzy = d.dungeonF7GoldorFrenzy;
                dungeonF7PurplePad = d.dungeonF7PurplePad;
                dungeonF7Simon = d.dungeonF7Simon;
                dungeonF7SimonProgress = d.dungeonF7SimonProgress;
                dungeonF7SimonFirstColor = d.dungeonF7SimonFirstColor;
                dungeonF7SimonSecondColor = d.dungeonF7SimonSecondColor;
                dungeonF7SimonOtherColor = d.dungeonF7SimonOtherColor;
                dungeonF7SimonBlockWrong = d.dungeonF7SimonBlockWrong;
                dungeonF7SimonAuto = d.dungeonF7SimonAuto;
                dungeonF7SimonTrigger = d.dungeonF7SimonTrigger;
                dungeonF7SimonSounds = d.dungeonF7SimonSounds;
                dungeonF7HideDiorite = d.dungeonF7HideDiorite;
                dungeonF7ArrowAlign = d.dungeonF7ArrowAlign;
                dungeonF7ArrowBlockWrong = d.dungeonF7ArrowBlockWrong;
                dungeonF7I4 = d.dungeonF7I4;
                dungeonF7I4Predict = d.dungeonF7I4Predict;
                dungeonF7I4PredictColor = d.dungeonF7I4PredictColor;
                dungeonF7AutoI4 = d.dungeonF7AutoI4;
                dungeonF7AutoI4Rod = d.dungeonF7AutoI4Rod;
                dungeonF7AutoI4Mask = d.dungeonF7AutoI4Mask;
                dungeonF7AutoI4Leap = d.dungeonF7AutoI4Leap;
                dungeonF7AutoI4LeapMelody = d.dungeonF7AutoI4LeapMelody;
                dungeonF7AutoI4LeapClass = d.dungeonF7AutoI4LeapClass;
                dungeonF7AutoI4Rotation = d.dungeonF7AutoI4Rotation;
                dungeonF7Debuff = d.dungeonF7Debuff;
                dungeonF7DebuffAuto = d.dungeonF7DebuffAuto;
                dungeonF7DebuffIce = d.dungeonF7DebuffIce;
                dungeonF7DebuffGravity = d.dungeonF7DebuffGravity;
                dungeonF7Gate = d.dungeonF7Gate;
                dungeonF7Relics = d.dungeonF7Relics;
                dungeonF7RelicLook = d.dungeonF7RelicLook;
                dungeonF7RelicLookTime = d.dungeonF7RelicLookTime;
                dungeonF7RelicSpawn = d.dungeonF7RelicSpawn;
                dungeonF7RelicSpawnTicks = d.dungeonF7RelicSpawnTicks;
                dungeonF7RelicBeacon = d.dungeonF7RelicBeacon;
                dungeonF7RelicPlace = d.dungeonF7RelicPlace;
                dungeonF7RelicHighlight = d.dungeonF7RelicHighlight;
                dungeonF7RelicBlockWrong = d.dungeonF7RelicBlockWrong;
                dungeonF7BreakerPreventSecrets = d.dungeonF7BreakerPreventSecrets;
                dungeonF7BreakerCharges = d.dungeonF7BreakerCharges;
                dungeonF7AutoSuperboom = d.dungeonF7AutoSuperboom;
                dungeonF7SuperboomSwapBack = d.dungeonF7SuperboomSwapBack;
                dungeonF7SuperboomDelay = d.dungeonF7SuperboomDelay;
                dungeonF7GateColor = d.dungeonF7GateColor;
                dungeonF7I4Color = d.dungeonF7I4Color;
                athen().copyModule(moduleId, d.athen());
            }
            case "qol.dungeon_menus" -> {
                dungeonMenusEnabled = d.dungeonMenusEnabled;
                dungeonMenusSalvage = d.dungeonMenusSalvage;
                dungeonMenusPartyFinder = d.dungeonMenusPartyFinder;
                dungeonMenusChestProfit = d.dungeonMenusChestProfit;
                dungeonMenusChestSpin = d.dungeonMenusChestSpin;
                dungeonMenusIncludeEssence = d.dungeonMenusIncludeEssence;
                dungeonMenusIncludeCost = d.dungeonMenusIncludeCost;
                dungeonMenusCompactProfit = d.dungeonMenusCompactProfit;
                dungeonMenusPartyCata = d.dungeonMenusPartyCata;
                dungeonMenusSalvage50Color = d.dungeonMenusSalvage50Color;
                dungeonMenusSalvageLowColor = d.dungeonMenusSalvageLowColor;
                dungeonMenusProfitColor = d.dungeonMenusProfitColor;
                dungeonMenusCloseChest = d.dungeonMenusCloseChest;
                dungeonMenusCloseChestMode = d.dungeonMenusCloseChestMode;
                athen().copyModule(moduleId, d.athen());
            }
            case "qol.camera" -> {
                cameraClip = d.cameraClip;
                cameraCustomDistance = d.cameraCustomDistance;
                cameraDistance = d.cameraDistance;
            }
            case "qol.slayer_display" -> {
                slayerDisplayEnabled = d.slayerDisplayEnabled;
                slayerDisplayKillTime = d.slayerDisplayKillTime;
                slayerDisplayDynamicSize = d.slayerDisplayDynamicSize;
            }
            case "qol.slayer_time_messages" -> {
                slayerTimeMessagesEnabled = d.slayerTimeMessagesEnabled;
                slayerTimeMessagesTimeToKill = d.slayerTimeMessagesTimeToKill;
                slayerTimeMessagesPersonalBest = d.slayerTimeMessagesPersonalBest;
                slayerTimeMessagesQuestComplete = d.slayerTimeMessagesQuestComplete;
                slayerTimeMessagesCompact = d.slayerTimeMessagesCompact;
            }
            case "qol.slayer_progress" -> {
                slayerProgressEnabled = d.slayerProgressEnabled;
                slayerProgressShowRemaining = d.slayerProgressShowRemaining;
                slayerProgressBossWarning = d.slayerProgressBossWarning;
                slayerProgressWarningRepeat = d.slayerProgressWarningRepeat;
                slayerProgressWarningPercent = d.slayerProgressWarningPercent;
            }
            case "qol.slayer_stats" -> {
                slayerStatsEnabled = d.slayerStatsEnabled;
                slayerStatsBossesKilled = d.slayerStatsBossesKilled;
                slayerStatsBossesPerHour = d.slayerStatsBossesPerHour;
                slayerStatsAverageKillTime = d.slayerStatsAverageKillTime;
                slayerStatsSessionTime = d.slayerStatsSessionTime;
            }
            case "qol.slayer_highlights" -> {
                slayerHighlightsEnabled = d.slayerHighlightsEnabled;
                slayerHighlightsOnlyMine = d.slayerHighlightsOnlyMine;
                slayerHighlightsBoss = d.slayerHighlightsBoss;
                slayerHighlightsMiniboss = d.slayerHighlightsMiniboss;
                slayerHighlightsDemon = d.slayerHighlightsDemon;
                slayerHighlightsDepth = d.slayerHighlightsDepth;
                slayerHighlightsBossColor = d.slayerHighlightsBossColor;
                slayerHighlightsMinibossColor = d.slayerHighlightsMinibossColor;
                slayerHighlightsDemonColor = d.slayerHighlightsDemonColor;
                slayerHighlightsBossWidth = d.slayerHighlightsBossWidth;
                slayerHighlightsMinibossWidth = d.slayerHighlightsMinibossWidth;
                slayerHighlightsDemonWidth = d.slayerHighlightsDemonWidth;
                slayerHighlightsTargetLines = d.slayerHighlightsTargetLines;
                slayerHighlightsTargetLineWidth = d.slayerHighlightsTargetLineWidth;
                slayerHighlightsTargetLineDistance = d.slayerHighlightsTargetLineDistance;
                slayerHighlightsHideSpawnParticles = d.slayerHighlightsHideSpawnParticles;
                slayerHighlightsHideDamageSplash = d.slayerHighlightsHideDamageSplash;
                slayerHighlightsHideMobNames = d.slayerHighlightsHideMobNames;
            }
            case "qol.slayer_active_boss_transparency" -> {
                slayerActiveBossTransparencyEnabled = d.slayerActiveBossTransparencyEnabled;
                slayerActiveBossTransparencyStrength = d.slayerActiveBossTransparencyStrength;
                slayerActiveBossTransparencyPlayers = d.slayerActiveBossTransparencyPlayers;
            }
            case "qol.slayer_irrelevant_mobs" -> {
                slayerIrrelevantMobsEnabled = d.slayerIrrelevantMobsEnabled;
                slayerIrrelevantMobsStrength = d.slayerIrrelevantMobsStrength;
            }
            case "qol.slayer_miniboss_alert" -> {
                slayerMinibossAlertEnabled = d.slayerMinibossAlertEnabled;
                slayerMinibossAlertMessage = d.slayerMinibossAlertMessage;
                slayerMinibossAlertTitle = d.slayerMinibossAlertTitle;
                slayerMinibossAlertDistance = d.slayerMinibossAlertDistance;
                slayerMinibossAlertText = d.slayerMinibossAlertText;
                slayerBigMinibossAlertText = d.slayerBigMinibossAlertText;
            }
            case "qol.slayer_drops" -> {
                slayerDropsEnabled = d.slayerDropsEnabled;
                slayerDropsShowChance = d.slayerDropsShowChance;
                slayerDropsBossesSince = d.slayerDropsBossesSince;
                slayerDropsDetectAutomatically = d.slayerDropsDetectAutomatically;
                slayerDropsRngHud = d.slayerDropsRngHud;
                slayerDropsRngWarnEmpty = d.slayerDropsRngWarnEmpty;
                slayerDropsRngHideChat = d.slayerDropsRngHideChat;
                slayerDropsProfitHud = d.slayerDropsProfitHud;
                slayerDropsProfitTable = d.slayerDropsProfitTable;
                slayerDropsProfitItemsShown = d.slayerDropsProfitItemsShown;
                slayerDropsProfitPerHour = d.slayerDropsProfitPerHour;
                slayerDropsProfitHideOutsideInventory = d.slayerDropsProfitHideOutsideInventory;
                slayerDropsGroundHighlight = d.slayerDropsGroundHighlight;
                slayerDropsGroundLabels = d.slayerDropsGroundLabels;
                slayerDropsGroundLabelMinimum = d.slayerDropsGroundLabelMinimum;
                slayerDropsPriceInChat = d.slayerDropsPriceInChat;
                slayerDropsPriceTitle = d.slayerDropsPriceTitle;
                slayerDropsPriceTitleSound = d.slayerDropsPriceTitleSound;
                slayerDropsPriceTitleMinimum = d.slayerDropsPriceTitleMinimum;
                slayerDropsRecentHighlight = d.slayerDropsRecentHighlight;
                slayerDropFilter = new java.util.ArrayList<>();
            }
            case "qol.slayer_carry" -> {
                slayerCarryEnabled = d.slayerCarryEnabled;
                slayerCarryAnnounceParty = d.slayerCarryAnnounceParty;
                slayerCarryShowSpawnMessage = d.slayerCarryShowSpawnMessage;
                slayerCarryDisplay = d.slayerCarryDisplay;
                slayerCarryWebhook = d.slayerCarryWebhook;
                slayerCarryWebhookEach = d.slayerCarryWebhookEach;
                slayerCarryWebhookUrl = d.slayerCarryWebhookUrl;
                slayerCarryVoidT3Prices = d.slayerCarryVoidT3Prices;
                slayerCarryVoidT4Prices = d.slayerCarryVoidT4Prices;
                slayerCarryInfernoT2Prices = d.slayerCarryInfernoT2Prices;
                slayerCarryInfernoT3Prices = d.slayerCarryInfernoT3Prices;
                slayerCarryInfernoT4Prices = d.slayerCarryInfernoT4Prices;
            }
            case "qol.slayer_cocoon_alert" -> {
                slayerCocoonAlertEnabled = d.slayerCocoonAlertEnabled;
                slayerCocoonShowAlert = d.slayerCocoonShowAlert;
                slayerCocoonAlertMessage = d.slayerCocoonAlertMessage;
                slayerCocoonAlertSound = d.slayerCocoonAlertSound;
                slayerCocoonAlertPitch = d.slayerCocoonAlertPitch;
                slayerCocoonAlertVolume = d.slayerCocoonAlertVolume;
                slayerCocoonTimer = d.slayerCocoonTimer;
            }
            case "qol.slayer_dagger_swap" -> {
                slayerDaggerSwapEnabled = d.slayerDaggerSwapEnabled;
                slayerDaggerSwapDelay = d.slayerDaggerSwapDelay;
                slayerDaggerSwapVariance = d.slayerDaggerSwapVariance;
            }
            case "qol.slayer_laser_hider" -> {
                slayerLaserHiderEnabled = d.slayerLaserHiderEnabled;
                slayerLaserShowForCarries = d.slayerLaserShowForCarries;
            }
            case "qol.slayer_attunement_display" -> {
                slayerAttunementDisplayEnabled = d.slayerAttunementDisplayEnabled;
                slayerAttunementDisplayCount = d.slayerAttunementDisplayCount;
            }
            case "qol.slayer_auto_soulcry" -> {
                slayerAutoSoulcryEnabled = d.slayerAutoSoulcryEnabled;
                slayerAutoSoulcryCheckMana = d.slayerAutoSoulcryCheckMana;
                slayerAutoSoulcryCheckHitbox = d.slayerAutoSoulcryCheckHitbox;
                slayerAutoSoulcryTickBased = d.slayerAutoSoulcryTickBased;
                slayerAutoSoulcryAttackBased = d.slayerAutoSoulcryAttackBased;
                slayerAutoSoulcryOtherBosses = d.slayerAutoSoulcryOtherBosses;
                slayerAutoSoulcryMinDelay = d.slayerAutoSoulcryMinDelay;
                slayerAutoSoulcryMaxDelay = d.slayerAutoSoulcryMaxDelay;
            }
            case "qol.slayer_sounds" -> {
                slayerSoundsEnabled = d.slayerSoundsEnabled;
                slayerSoundsDisableVoidgloom = d.slayerSoundsDisableVoidgloom;
                slayerSoundsDisableVampire = d.slayerSoundsDisableVampire;
            }
            case "qol.slayer_voidgloom" -> {
                slayerVoidgloomEnabled = d.slayerVoidgloomEnabled;
                slayerVoidgloomHighlightBeacon = d.slayerVoidgloomHighlightBeacon;
                slayerVoidgloomBeaconWarning = d.slayerVoidgloomBeaconWarning;
                slayerVoidgloomBeaconSound = d.slayerVoidgloomBeaconSound;
                slayerVoidgloomBeaconLine = d.slayerVoidgloomBeaconLine;
                slayerVoidgloomBeaconPath = d.slayerVoidgloomBeaconPath;
                slayerVoidgloomBeaconTimer = d.slayerVoidgloomBeaconTimer;
                slayerVoidgloomBeaconColor = d.slayerVoidgloomBeaconColor;
                slayerVoidgloomLineColor = d.slayerVoidgloomLineColor;
                slayerVoidgloomLineWidth = d.slayerVoidgloomLineWidth;
                slayerVoidgloomHighlightHeld = d.slayerVoidgloomHighlightHeld;
                slayerVoidgloomHighlightNukekubi = d.slayerVoidgloomHighlightNukekubi;
                slayerVoidgloomNukekubiLine = d.slayerVoidgloomNukekubiLine;
                slayerVoidgloomNukekubiColor = d.slayerVoidgloomNukekubiColor;
                slayerVoidgloomWorldLabels = d.slayerVoidgloomWorldLabels;
                slayerVoidgloomLineToBoss = d.slayerVoidgloomLineToBoss;
                slayerVoidgloomBossLineWidth = d.slayerVoidgloomBossLineWidth;
                slayerVoidgloomPhaseDisplay = d.slayerVoidgloomPhaseDisplay;
                slayerVoidgloomHitsDisplay = d.slayerVoidgloomHitsDisplay;
                slayerVoidgloomLaserTimer = d.slayerVoidgloomLaserTimer;
                slayerVoidgloomLaserHealth = d.slayerVoidgloomLaserHealth;
                slayerVoidgloomHideParticles = d.slayerVoidgloomHideParticles;
            }
            case "qol.slayer_revenant" -> {
                slayerRevenantEnabled = d.slayerRevenantEnabled;
                slayerRevenantBoomDisplay = d.slayerRevenantBoomDisplay;
                slayerRevenantBoomSound = d.slayerRevenantBoomSound;
                slayerRevenantBoomHighlight = d.slayerRevenantBoomHighlight;
                slayerRevenantBoomColor = d.slayerRevenantBoomColor;
                slayerRevenantWorldLabels = d.slayerRevenantWorldLabels;
                slayerRevenantLineToBoss = d.slayerRevenantLineToBoss;
            }
            case "qol.slayer_tarantula" -> {
                slayerTarantulaEnabled = d.slayerTarantulaEnabled;
                slayerTarantulaHighlightEggSacs = d.slayerTarantulaHighlightEggSacs;
                slayerTarantulaEggColor = d.slayerTarantulaEggColor;
                slayerTarantulaEggHits = d.slayerTarantulaEggHits;
                slayerTarantulaHighlightInvincible = d.slayerTarantulaHighlightInvincible;
                slayerTarantulaInvincibleColor = d.slayerTarantulaInvincibleColor;
                slayerTarantulaInvincibleText = d.slayerTarantulaInvincibleText;
                slayerTarantulaPhaseDisplay = d.slayerTarantulaPhaseDisplay;
                slayerTarantulaWorldLabels = d.slayerTarantulaWorldLabels;
                slayerTarantulaMuteSounds = d.slayerTarantulaMuteSounds;
                slayerTarantulaLineToBoss = d.slayerTarantulaLineToBoss;
                slayerTarantulaBossLineWidth = d.slayerTarantulaBossLineWidth;
            }
            case "qol.slayer_sven" -> {
                slayerSvenEnabled = d.slayerSvenEnabled;
                slayerSvenHighlightPups = d.slayerSvenHighlightPups;
                slayerSvenPupColor = d.slayerSvenPupColor;
                slayerSvenPupLine = d.slayerSvenPupLine;
                slayerSvenWorldLabels = d.slayerSvenWorldLabels;
                slayerSvenHidePupNametags = d.slayerSvenHidePupNametags;
                slayerSvenHowlWarning = d.slayerSvenHowlWarning;
                slayerSvenMuteSounds = d.slayerSvenMuteSounds;
                slayerSvenLineToBoss = d.slayerSvenLineToBoss;
            }
            case "qol.slayer_vampire_markers" -> {
                slayerVampireMarkersEnabled = d.slayerVampireMarkersEnabled;
                slayerVampireMarkersBloodIchor = d.slayerVampireMarkersBloodIchor;
                slayerVampireMarkersIchorColor = d.slayerVampireMarkersIchorColor;
                slayerVampireMarkersKillerSpring = d.slayerVampireMarkersKillerSpring;
                slayerVampireMarkersSpringColor = d.slayerVampireMarkersSpringColor;
                slayerVampireMarkersTwinclaws = d.slayerVampireMarkersTwinclaws;
                slayerVampireMarkersTwinclawsDelay = d.slayerVampireMarkersTwinclawsDelay;
                slayerVampireMarkersMania = d.slayerVampireMarkersMania;
                slayerVampireMarkersManiaTimer = d.slayerVampireMarkersManiaTimer;
                slayerVampireMarkersSteakAlert = d.slayerVampireMarkersSteakAlert;
                slayerVampireMarkersSteakColor = d.slayerVampireMarkersSteakColor;
                slayerVampireMarkersWorldLabels = d.slayerVampireMarkersWorldLabels;
                slayerVampireMarkersMuteSounds = d.slayerVampireMarkersMuteSounds;
                slayerVampireMarkersLineToBoss = d.slayerVampireMarkersLineToBoss;
                slayerVampireMarkersBossLineWidth = d.slayerVampireMarkersBossLineWidth;
                slayerVampireMarkersIchorBeam = d.slayerVampireMarkersIchorBeam;
                slayerVampireMarkersChalice = d.slayerVampireMarkersChalice;
                slayerVampireMarkersChaliceColor = d.slayerVampireMarkersChaliceColor;
                slayerVampireMarkersEffigies = d.slayerVampireMarkersEffigies;
            }
            case "qol.slayer_inferno" -> {
                slayerInfernoEnabled = d.slayerInfernoEnabled;
                slayerInfernoFirePillar = d.slayerInfernoFirePillar;
                slayerInfernoFirePillarSound = d.slayerInfernoFirePillarSound;
                slayerInfernoPillarColor = d.slayerInfernoPillarColor;
                slayerInfernoFirePits = d.slayerInfernoFirePits;
                slayerInfernoPhaseDisplay = d.slayerInfernoPhaseDisplay;
                slayerInfernoColorByAttunement = d.slayerInfernoColorByAttunement;
                slayerInfernoHideChat = d.slayerInfernoHideChat;
                slayerInfernoHideParticles = d.slayerInfernoHideParticles;
                slayerInfernoWorldLabels = d.slayerInfernoWorldLabels;
                slayerInfernoLineToBoss = d.slayerInfernoLineToBoss;
                slayerInfernoGummyWarning = d.slayerInfernoGummyWarning;
            }
            case "qol.slayer_quest_warning" -> {
                slayerQuestWarningEnabled = d.slayerQuestWarningEnabled;
                slayerQuestWarningTitle = d.slayerQuestWarningTitle;
                slayerQuestWarningChat = d.slayerQuestWarningChat;
            }
            case "qol.slayer_auto_start" -> {
                slayerAutoStartEnabled = d.slayerAutoStartEnabled;
                slayerAutoStartDelay = d.slayerAutoStartDelay;
                slayerAutoStartBlockNotSpawnable = d.slayerAutoStartBlockNotSpawnable;
            }
            case "qol.slayer_vengeance" -> {
                slayerVengeanceEnabled = d.slayerVengeanceEnabled;
                slayerVengeanceCompact = d.slayerVengeanceCompact;
                slayerVengeanceUseTicks = d.slayerVengeanceUseTicks;
            }
            case "qol.slayer_vengeance_damage" -> {
                slayerVengeanceDamageEnabled = d.slayerVengeanceDamageEnabled;
                slayerVengeanceDamageAbbreviate = d.slayerVengeanceDamageAbbreviate;
            }
            case "qol.slayer_big_drops" -> {
                slayerBigDropsEnabled = d.slayerBigDropsEnabled;
                slayerBigDropsScale = d.slayerBigDropsScale;
                slayerBigDropsRange = d.slayerBigDropsRange;
                slayerBigDropsUnscaleSeconds = d.slayerBigDropsUnscaleSeconds;
                slayerBigDropsRevenant = d.slayerBigDropsRevenant;
                slayerBigDropsTarantula = d.slayerBigDropsTarantula;
                slayerBigDropsSven = d.slayerBigDropsSven;
                slayerBigDropsVoidgloom = d.slayerBigDropsVoidgloom;
                slayerBigDropsInferno = d.slayerBigDropsInferno;
                slayerBigDropsVampire = d.slayerBigDropsVampire;
            }
            case "qol.storage_overlay" -> {
                storageOverlayEnabled = d.storageOverlayEnabled;
                storageOverlayAlwaysOpen = d.storageOverlayAlwaysOpen;
                storageOverlayOutlineActive = d.storageOverlayOutlineActive;
                storageOverlayOutlineColor = d.storageOverlayOutlineColor;
                storageOverlayInactiveTooltips = d.storageOverlayInactiveTooltips;
                storageOverlayColumns = d.storageOverlayColumns;
                storageOverlayHeight = d.storageOverlayHeight;
                storageOverlayRetainScroll = d.storageOverlayRetainScroll;
                storageOverlayScrollSpeed = d.storageOverlayScrollSpeed;
                storageOverlayInvertScroll = d.storageOverlayInvertScroll;
                storageOverlayPadding = d.storageOverlayPadding;
                storageOverlayMargin = d.storageOverlayMargin;
                storageOverlayBlockItemScroll = d.storageOverlayBlockItemScroll;
                storageOverlayHighlightSearch = d.storageOverlayHighlightSearch;
                storageOverlayFilterSearch = d.storageOverlayFilterSearch;
                storageItemSearch = d.storageItemSearch;
                storageCraftHelper = d.storageCraftHelper;
                storageMuseumArmor = d.storageMuseumArmor;
                storageItemSearchKeybind = d.storageItemSearchKeybind;
                storageOverlayHighlightColor = d.storageOverlayHighlightColor;
                storageOverlayPanelColor = d.storageOverlayPanelColor;
                storageOverlayCardColor = d.storageOverlayCardColor;
                storageOverlayCardActiveColor = d.storageOverlayCardActiveColor;
                storageOverlayPlayerColor = d.storageOverlayPlayerColor;
            }
            case "qol.inventory_buttons" -> {
                inventoryButtonsEnabled = d.inventoryButtonsEnabled;
                inventoryButtonsHoverTooltip = d.inventoryButtonsHoverTooltip;
                inventoryButtonsInventoryOnly = d.inventoryButtonsInventoryOnly;
                inventoryButtons = new java.util.ArrayList<>();
                inventoryButtonsSavedPreset = new java.util.ArrayList<>();
                for (InventoryButtonsPolicy.Button button : InventoryButtonsPolicy.simplePreset()) {
                    inventoryButtons.add(button.copy());
                }
            }
            case "qol.reward_claim" -> {
                rewardClaimEnabled = d.rewardClaimEnabled;
                rewardClaimMuteChatLink = d.rewardClaimMuteChatLink;
                rewardClaimBlockBrowser = d.rewardClaimBlockBrowser;
                rewardClaimWaitForAd = d.rewardClaimWaitForAd;
            }
            case "qol.render_optimizer" -> {
                hideEmptyTooltips = d.hideEmptyTooltips;
                hideBreakParticles = d.hideBreakParticles;
                hideBossBar = d.hideBossBar;
                hideArmorBar = d.hideArmorBar;
                hideFoodBar = d.hideFoodBar;
                hideFog = d.hideFog;
                hideEffectDisplay = d.hideEffectDisplay;
                hideRecipeBook = d.hideRecipeBook;
                hideSelectedItemName = d.hideSelectedItemName;
                hideDeadEntities = d.hideDeadEntities;
                hideDeadPoof = d.hideDeadPoof;
                hideImplosionParticles = d.hideImplosionParticles;
                hideEntityFire = d.hideEntityFire;
                hideMageBeam = d.hideMageBeam;
                hideIceSpray = d.hideIceSpray;
                hidePowderCoating = d.hidePowderCoating;
                hideGuidedSheep = d.hideGuidedSheep;
                hideBonePlating = d.hideBonePlating;
                hideTreeBits = d.hideTreeBits;
                hideNausea = d.hideNausea;
                hideStuckArrows = d.hideStuckArrows;
                vignetteMode = d.vignetteMode;
                hideIslandClouds = d.hideIslandClouds;
                netherFog = d.netherFog;
                netherFogScale = d.netherFogScale;
                totemAnimation = d.totemAnimation;
                mobIcons = d.mobIcons;
                armorSelf = d.armorSelf;
                armorOthers = d.armorOthers;
                fullTextShadow = d.fullTextShadow;
            }
            case "qol.farm_keys" -> {
                farmKeysEnabled = d.farmKeysEnabled;
                farmKeysLockCamera = d.farmKeysLockCamera;
                farmKeysAttack = d.farmKeysAttack;
                farmKeysJump = d.farmKeysJump;
                farmKeysPrevAttack = d.farmKeysPrevAttack;
                farmKeysPrevJump = d.farmKeysPrevJump;
            }
            case "qol.freecam" -> {
                freecamEnabled = d.freecamEnabled;
                freecamSpeed = d.freecamSpeed;
                freecamShowBody = d.freecamShowBody;
                freecamCollide = d.freecamCollide;
                freecamKeybind = d.freecamKeybind;
            }
            case "qol.hud_layout" -> {
                hudLayoutEnabled = d.hudLayoutEnabled;
                hudLayoutShowBackground = d.hudLayoutShowBackground;
                hudLayoutDimUnfocused = d.hudLayoutDimUnfocused;
                hudHideHotbar = d.hudHideHotbar;
                hudHideHealth = d.hudHideHealth;
                hudHideFood = d.hudHideFood;
                hudHideArmor = d.hudHideArmor;
                hudHideXp = d.hudHideXp;
                hudHideAir = d.hudHideAir;
                hudHideMount = d.hudHideMount;
                hudHideScoreboard = d.hudHideScoreboard;
                hudHideBoss = d.hudHideBoss;
                hudHideAction = d.hudHideAction;
                hudHideItemName = d.hudHideItemName;
                hudHideEffects = d.hudHideEffects;
                hudHideTitles = d.hudHideTitles;
                hudHideTab = d.hudHideTab;
                hudLayoutBackgroundColor = d.hudLayoutBackgroundColor;
                hudLayoutTextColor = d.hudLayoutTextColor;
                hudLayoutScale = d.hudLayoutScale;
                hudStyles = new LinkedHashMap<>();
                hudEditorTitleX = d.hudEditorTitleX;
                hudEditorTitleY = d.hudEditorTitleY;
                hudEditorHelpX = d.hudEditorHelpX;
                hudEditorHelpY = d.hudEditorHelpY;
                hudEditorInspectorX = d.hudEditorInspectorX;
                hudEditorInspectorY = d.hudEditorInspectorY;
            }
            case "qol.custom_cursor" -> {
                customCursorEnabled = d.customCursorEnabled;
                customCursorSize = d.customCursorSize;
                customCursorFill = d.customCursorFill;
                customCursorOutline = d.customCursorOutline;
                customCursorAccent = d.customCursorAccent;
                customCursorClickAnim = d.customCursorClickAnim;
                customCursorHoldAnim = d.customCursorHoldAnim;
                customCursorHideVanilla = d.customCursorHideVanilla;
            }
            case "qol.legacy_textures" -> {
                legacyTexturesEnabled = d.legacyTexturesEnabled;
                legacyTexturesItems = d.legacyTexturesItems;
            }
            case "qol.custom_resource_pack" -> {
                customResourcePackEnabled = d.customResourcePackEnabled;
                customResourcePackOverworld = d.customResourcePackOverworld;
                customResourcePackCrimson = d.customResourcePackCrimson;
                customResourcePackEnd = d.customResourcePackEnd;
                customResourcePackGameplayFont = d.customResourcePackGameplayFont;
            }
            case "qol.iota" -> {
                iotaAddonsEnabled = d.iotaAddonsEnabled;
                iotaPartyJoinSound = d.iotaPartyJoinSound;
                iotaLimboAlert = d.iotaLimboAlert;
                iotaFixFishingHook = d.iotaFixFishingHook;
                iotaMuteFishingCast = d.iotaMuteFishingCast;
                iotaMuteTerminator = d.iotaMuteTerminator;
                iotaPartyCommands = d.iotaPartyCommands;
                iotaPartyWarp = d.iotaPartyWarp;
                iotaPartyTransfer = d.iotaPartyTransfer;
                iotaPartyPing = d.iotaPartyPing;
                iotaPartyAllInvite = d.iotaPartyAllInvite;
                iotaPartyTps = d.iotaPartyTps;
                iotaPartyPromote = d.iotaPartyPromote;
                iotaPartyKick = d.iotaPartyKick;
                iotaPartyKuudra = d.iotaPartyKuudra;
                iotaPartyChests = d.iotaPartyChests;
                iotaPartyRuns = d.iotaPartyRuns;
                iotaPartyProfit = d.iotaPartyProfit;
                iotaArrowTracker = d.iotaArrowTracker;
                iotaArrowNotifications = d.iotaArrowNotifications;
                iotaArrowVisibility = d.iotaArrowVisibility;
                iotaAutoRequeue = d.iotaAutoRequeue;
                iotaToggleLeftKeybind = d.iotaToggleLeftKeybind;
                iotaToggleRightKeybind = d.iotaToggleRightKeybind;
                iotaSupplyWaypoints = d.iotaSupplyWaypoints;
                iotaSupplyHitbox = d.iotaSupplyHitbox;
                iotaSupplyPullCircle = d.iotaSupplyPullCircle;
                iotaSupplyGiantHitbox = d.iotaSupplyGiantHitbox;
                iotaPileWaypoints = d.iotaPileWaypoints;
                iotaPileNames = d.iotaPileNames;
                iotaPearlWaypoints = d.iotaPearlWaypoints;
                iotaBuildWaypoints = d.iotaBuildWaypoints;
                iotaStunWaypoints = d.iotaStunWaypoints;
                iotaStunPod = d.iotaStunPod;
                iotaIchorPool = d.iotaIchorPool;
                iotaKuudraHitbox = d.iotaKuudraHitbox;
                iotaEtherwarpHelper = d.iotaEtherwarpHelper;
                iotaFreshTools = d.iotaFreshTools;
                iotaFreshAnnounce = d.iotaFreshAnnounce;
                iotaFreshParty = d.iotaFreshParty;
                iotaBuildInfo = d.iotaBuildInfo;
                iotaKuudraTitles = d.iotaKuudraTitles;
                iotaAlertHudX = d.iotaAlertHudX;
                iotaAlertHudY = d.iotaAlertHudY;
            }
            case "qol.stall_market" -> {
                stallMarketEnabled = d.stallMarketEnabled;
                stallBazaarSearch = d.stallBazaarSearch;
                stallSellProtection = d.stallSellProtection;
                stallSellThreshold = d.stallSellThreshold;
                stallAngryCoop = d.stallAngryCoop;
                stallBinOverlay = d.stallBinOverlay;
                stallAhHighlight = d.stallAhHighlight;
                stallSearchKeybind = d.stallSearchKeybind;
            }
            case "qol.custom_scoreboard" -> board().copyFrom(d.board());
            default -> {
                if (!athen().handlesModule(moduleId)) {
                    return false;
                }
                athen().copyModule(moduleId, d.athen());
            }
        }
        return true;
    }

    String readText(String settingId) {
        if (settingId != null && settingId.startsWith("qol.custom_scoreboard.")) {
            String value = board().readText(settingId);
            if (value != null) {
                return value;
            }
        }
        if ("qol.dungeon_leap.message".equals(settingId)) {
            return dungeonLeapMessage == null || dungeonLeapMessage.isBlank()
                    ? "ILY {name}" : dungeonLeapMessage;
        }
        if ("qol.dungeon_announce.melody_message".equals(settingId)) {
            return dungeonAnnounceMelodyMessage == null || dungeonAnnounceMelodyMessage.isBlank()
                    ? DungeonBladePolicy.DEFAULT_MELODY_PARTY : dungeonAnnounceMelodyMessage;
        }
        if ("qol.dungeon_announce.death_message".equals(settingId)) {
            return dungeonAnnounceDeathMessage == null || dungeonAnnounceDeathMessage.isBlank()
                    ? DungeonBladePolicy.DEFAULT_DEATH_MESSAGE : dungeonAnnounceDeathMessage;
        }
        if ("qol.dungeon_f7.title_crystal_text".equals(settingId)) {
            return safe(dungeonF7TitleCrystalText);
        }
        if ("qol.dungeon_f7.title_wither_text".equals(settingId)) {
            return safe(dungeonF7TitleWitherText);
        }
        if ("qol.dungeon_f7.title_terminal_text".equals(settingId)) {
            return safe(dungeonF7TitleTerminalText);
        }
        if ("qol.dungeon_f7.title_gate_text".equals(settingId)) {
            return safe(dungeonF7TitleGateText);
        }
        if ("qol.auto_sell.list".equals(settingId)) {
            return AutoSellPolicy.formatList(autoSellItems);
        }
        if ("qol.slayer_miniboss_alert.text".equals(settingId)) {
            return slayerMinibossAlertText == null ? "" : slayerMinibossAlertText;
        }
        if ("qol.slayer_miniboss_alert.big_text".equals(settingId)) {
            return slayerBigMinibossAlertText == null ? "" : slayerBigMinibossAlertText;
        }
        if ("qol.slayer_cocoon_alert.message".equals(settingId)) {
            return slayerCocoonAlertMessage == null ? "" : slayerCocoonAlertMessage;
        }
        if ("qol.slayer_cocoon_alert.sound".equals(settingId)) {
            return slayerCocoonAlertSound == null ? "" : slayerCocoonAlertSound;
        }
        if ("qol.slayer_carry.webhook_url".equals(settingId)) return safe(slayerCarryWebhookUrl);
        if ("qol.slayer_carry.void_t3_prices".equals(settingId)) return safe(slayerCarryVoidT3Prices);
        if ("qol.slayer_carry.void_t4_prices".equals(settingId)) return safe(slayerCarryVoidT4Prices);
        if ("qol.slayer_carry.inferno_t2_prices".equals(settingId)) return safe(slayerCarryInfernoT2Prices);
        if ("qol.slayer_carry.inferno_t3_prices".equals(settingId)) return safe(slayerCarryInfernoT3Prices);
        if ("qol.slayer_carry.inferno_t4_prices".equals(settingId)) return safe(slayerCarryInfernoT4Prices);
        if ("qol.storage_overlay.search_query".equals(settingId)) return safe(storageOverlaySearchQuery);
        return athen().readText(settingId);
    }

    boolean writeText(String settingId, String value) {
        if (settingId != null && settingId.startsWith("qol.custom_scoreboard.")
                && board().writeText(settingId, value)) {
            return true;
        }
        if ("qol.dungeon_leap.message".equals(settingId)) {
            dungeonLeapMessage = value == null || value.isBlank() ? "ILY {name}" : value;
            return true;
        }
        if ("qol.dungeon_announce.melody_message".equals(settingId)) {
            dungeonAnnounceMelodyMessage = value == null || value.isBlank()
                    ? DungeonBladePolicy.DEFAULT_MELODY_PARTY : value;
            return true;
        }
        if ("qol.dungeon_announce.death_message".equals(settingId)) {
            dungeonAnnounceDeathMessage = value == null || value.isBlank()
                    ? DungeonBladePolicy.DEFAULT_DEATH_MESSAGE : value;
            return true;
        }
        if ("qol.dungeon_f7.title_crystal_text".equals(settingId)) {
            dungeonF7TitleCrystalText = value == null ? "" : value;
            return true;
        }
        if ("qol.dungeon_f7.title_wither_text".equals(settingId)) {
            dungeonF7TitleWitherText = value == null ? "" : value;
            return true;
        }
        if ("qol.dungeon_f7.title_terminal_text".equals(settingId)) {
            dungeonF7TitleTerminalText = value == null ? "" : value;
            return true;
        }
        if ("qol.dungeon_f7.title_gate_text".equals(settingId)) {
            dungeonF7TitleGateText = value == null ? "" : value;
            return true;
        }
        if ("qol.auto_sell.list".equals(settingId)) {
            autoSellItems = new java.util.ArrayList<>(AutoSellPolicy.parseList(value));
            return true;
        }
        if ("qol.slayer_miniboss_alert.text".equals(settingId)) {
            slayerMinibossAlertText = value == null ? "" : value.trim();
            return true;
        }
        if ("qol.slayer_miniboss_alert.big_text".equals(settingId)) {
            slayerBigMinibossAlertText = value == null ? "" : value.trim();
            return true;
        }
        if ("qol.slayer_cocoon_alert.message".equals(settingId)) {
            slayerCocoonAlertMessage = value == null ? "" : value.trim();
            return true;
        }
        if ("qol.slayer_cocoon_alert.sound".equals(settingId)) {
            slayerCocoonAlertSound = value == null ? "" : value.trim();
            return true;
        }
        String stored = value == null ? "" : value.trim();
        if ("qol.slayer_carry.webhook_url".equals(settingId)) {
            slayerCarryWebhookUrl = SlayerCarryPolicy.sanitizeWebhookUrl(stored);
            return true;
        }
        if ("qol.slayer_carry.void_t3_prices".equals(settingId)) { slayerCarryVoidT3Prices = stored; return true; }
        if ("qol.slayer_carry.void_t4_prices".equals(settingId)) { slayerCarryVoidT4Prices = stored; return true; }
        if ("qol.slayer_carry.inferno_t2_prices".equals(settingId)) { slayerCarryInfernoT2Prices = stored; return true; }
        if ("qol.slayer_carry.inferno_t3_prices".equals(settingId)) { slayerCarryInfernoT3Prices = stored; return true; }
        if ("qol.slayer_carry.inferno_t4_prices".equals(settingId)) { slayerCarryInfernoT4Prices = stored; return true; }
        if ("qol.storage_overlay.search_query".equals(settingId)) { storageOverlaySearchQuery = stored; return true; }
        return athen().writeText(settingId, value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    String readKeybind(String settingId) {
        if ("qol.custom_scoreboard.keybind".equals(settingId)) {
            String value = board().keybind;
            return value == null ? "" : value;
        }
        return switch (settingId) {
            case "qol.cheater_wardrobe.slot_1" -> cheaterWardrobeSlot1;
            case "qol.cheater_wardrobe.slot_2" -> cheaterWardrobeSlot2;
            case "qol.cheater_wardrobe.slot_3" -> cheaterWardrobeSlot3;
            case "qol.cheater_wardrobe.slot_4" -> cheaterWardrobeSlot4;
            case "qol.cheater_wardrobe.slot_5" -> cheaterWardrobeSlot5;
            case "qol.cheater_wardrobe.slot_6" -> cheaterWardrobeSlot6;
            case "qol.cheater_wardrobe.slot_7" -> cheaterWardrobeSlot7;
            case "qol.cheater_wardrobe.slot_8" -> cheaterWardrobeSlot8;
            case "qol.cheater_wardrobe.slot_9" -> cheaterWardrobeSlot9;
            case "qol.auto_gfs.keybind" -> autoGfsKeybind == null ? "" : autoGfsKeybind;
            case "qol.auto_sell.keybind" -> autoSellKeybind == null ? "" : autoSellKeybind;
            case "qol.ghosts.keybind" -> ghostsKeybind == null ? "" : ghostsKeybind;
            case "qol.freecam.keybind" -> freecamKeybind == null ? "" : freecamKeybind;
            case "qol.dungeon_termsim.keybind" -> dungeonTermSimKeybind == null ? "" : dungeonTermSimKeybind;
            case "qol.dungeon_esp.ghost_keybind" -> dungeonEspGhostKeybind == null ? "" : dungeonEspGhostKeybind;
            case "qol.farm_keys.attack" -> farmKeysAttack == null ? "" : farmKeysAttack;
            case "qol.farm_keys.jump" -> farmKeysJump == null ? "" : farmKeysJump;
            case "qol.iota.toggle_left" -> iotaToggleLeftKeybind == null ? "" : iotaToggleLeftKeybind;
            case "qol.iota.toggle_right" -> iotaToggleRightKeybind == null ? "" : iotaToggleRightKeybind;
            case "qol.stall_market.search_keybind" -> stallSearchKeybind == null ? "" : stallSearchKeybind;
            case "qol.storage_overlay.item_search_keybind" -> storageItemSearchKeybind == null ? "" : storageItemSearchKeybind;
            default -> athen().readKeybind(settingId);
        };
    }

    boolean writeKeybind(String settingId, String value) {
        String stored = value == null ? "" : value.trim();
        if ("qol.custom_scoreboard.keybind".equals(settingId)) {
            board().keybind = stored;
            return true;
        }
        switch (settingId) {
            case "qol.cheater_wardrobe.slot_1" -> cheaterWardrobeSlot1 = stored;
            case "qol.cheater_wardrobe.slot_2" -> cheaterWardrobeSlot2 = stored;
            case "qol.cheater_wardrobe.slot_3" -> cheaterWardrobeSlot3 = stored;
            case "qol.cheater_wardrobe.slot_4" -> cheaterWardrobeSlot4 = stored;
            case "qol.cheater_wardrobe.slot_5" -> cheaterWardrobeSlot5 = stored;
            case "qol.cheater_wardrobe.slot_6" -> cheaterWardrobeSlot6 = stored;
            case "qol.cheater_wardrobe.slot_7" -> cheaterWardrobeSlot7 = stored;
            case "qol.cheater_wardrobe.slot_8" -> cheaterWardrobeSlot8 = stored;
            case "qol.cheater_wardrobe.slot_9" -> cheaterWardrobeSlot9 = stored;
            case "qol.auto_gfs.keybind" -> autoGfsKeybind = stored;
            case "qol.auto_sell.keybind" -> autoSellKeybind = stored;
            case "qol.ghosts.keybind" -> ghostsKeybind = stored;
            case "qol.freecam.keybind" -> freecamKeybind = stored;
            case "qol.dungeon_termsim.keybind" -> dungeonTermSimKeybind = stored;
            case "qol.dungeon_esp.ghost_keybind" -> dungeonEspGhostKeybind = stored;
            case "qol.farm_keys.attack" -> farmKeysAttack = stored;
            case "qol.farm_keys.jump" -> farmKeysJump = stored;
            case "qol.iota.toggle_left" -> iotaToggleLeftKeybind = stored;
            case "qol.iota.toggle_right" -> iotaToggleRightKeybind = stored;
            case "qol.stall_market.search_keybind" -> stallSearchKeybind = stored;
            case "qol.storage_overlay.item_search_keybind" -> storageItemSearchKeybind = stored;
            default -> {
                if (!athen().writeKeybind(settingId, stored)) {
                    return false;
                }
            }
        }
        return true;
    }

    IotaPolicy.PartyCommandConfig partyCommandConfig() {
        return new IotaPolicy.PartyCommandConfig(
                iotaPartyCommands,
                iotaPartyWarp,
                iotaPartyTransfer,
                iotaPartyPing,
                iotaPartyAllInvite,
                iotaPartyTps,
                iotaPartyPromote,
                iotaPartyKick,
                iotaPartyKuudra,
                iotaPartyChests,
                iotaPartyRuns,
                iotaPartyProfit);
    }

    boolean addAutoSellDefaults() {
        autoSellItems = new java.util.ArrayList<>(AutoSellPolicy.withDefaults(autoSellItems));
        return true;
    }

    void rememberPricePaid(String uuid, long coins) {
        if (uuid == null || uuid.isBlank() || coins <= 0L) {
            return;
        }
        if (pricePaidByUuid == null) {
            pricePaidByUuid = new LinkedHashMap<>();
        }
        pricePaidByUuid.put(uuid, coins);
        while (pricePaidByUuid.size() > 400) {
            String first = pricePaidByUuid.keySet().iterator().next();
            pricePaidByUuid.remove(first);
        }
    }

    long pricePaidFor(String uuid) {
        if (uuid == null || uuid.isBlank() || pricePaidByUuid == null) {
            return 0L;
        }
        Long value = pricePaidByUuid.get(uuid);
        return value == null ? 0L : value;
    }

    static String normalizeVignette(String value) {
        if (value == null) {
            return VIGNETTE_NONE;
        }
        String text = value.trim();
        if (text.equalsIgnoreCase(VIGNETTE_AMBIENT)) {
            return VIGNETTE_AMBIENT;
        }
        if (text.equalsIgnoreCase(VIGNETTE_DANGER)) {
            return VIGNETTE_DANGER;
        }
        if (text.equalsIgnoreCase(VIGNETTE_BOTH)) {
            return VIGNETTE_BOTH;
        }
        return VIGNETTE_NONE;
    }

    static String normalizeStyle(String value) {
        if (value == null) {
            return STYLE_SIMPLE;
        }
        String text = value.trim();
        if (text.equalsIgnoreCase(STYLE_HOTBAR)) {
            return STYLE_HOTBAR;
        }
        if (text.equalsIgnoreCase(STYLE_CUSTOM)) {
            return STYLE_CUSTOM;
        }
        return STYLE_SIMPLE;
    }

    static String normalizeId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    HudStyleState resolvedHudStyle(String elementId) {
        HudStyleState global = new HudStyleState();
        global.showBackground = hudLayoutShowBackground;
        global.showTitle = Boolean.TRUE;
        global.backgroundColor = hudLayoutBackgroundColor;
        global.textColor = hudLayoutTextColor;
        global.scale = HudStylePolicy.clampScale(hudLayoutScale);
        if (hudStyles == null || elementId == null || elementId.isBlank()) {
            return global;
        }
        HudStyleState override = hudStyles.get(normalizeId(elementId));
        return HudStylePolicy.resolve(global, override);
    }

    void putHudStyle(String elementId, HudStyleState style) {
        if (elementId == null || elementId.isBlank() || style == null) {
            return;
        }
        if (hudStyles == null) {
            hudStyles = new LinkedHashMap<>();
        }
        hudStyles.put(normalizeId(elementId), HudStylePolicy.copy(style));
    }

    void resetHudStyle(String elementId) {
        if (hudStyles == null || elementId == null || elementId.isBlank()) {
            return;
        }
        hudStyles.remove(normalizeId(elementId));
    }

    void resetHudEditorChrome() {
        hudEditorTitleX = HudEditorChromePolicy.UNPLACED;
        hudEditorTitleY = HudEditorChromePolicy.UNPLACED;
        hudEditorHelpX = HudEditorChromePolicy.UNPLACED;
        hudEditorHelpY = HudEditorChromePolicy.UNPLACED;
        hudEditorInspectorX = HudEditorChromePolicy.UNPLACED;
        hudEditorInspectorY = HudEditorChromePolicy.UNPLACED;
    }
}
