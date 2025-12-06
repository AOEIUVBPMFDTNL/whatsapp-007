package com.whatsapp.android.service.impl.call;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.XmppDecode;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import com.whatsapp.android.entity.TurnIpData;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class TE2IpParser {

    /**
     * IPv4地址长度（4字节IP + 2字节端口）
     */
    private static final int IPV4_DATA_LENGTH = 6;
    /**
     * IPv6地址长度（16字节IP + 2字节端口）
     */
    private static final int IPV6_DATA_LENGTH = 18;

    /**
     * 解析TURN IP数据
     *
     * @param tokensNode token节点列表
     * @param te2Node    TE2节点列表
     * @return 解析后的TurnIpData，如果没有匹配的数据则返回null
     */
    public TurnIpData parseTurnIpData(LinkedList<ProtocolTreeNode> tokensNode, LinkedList<ProtocolTreeNode> te2Node) {
        if (tokensNode == null || te2Node == null || tokensNode.isEmpty() || te2Node.isEmpty()) {
            return null;
        }
        try {
            // 构建token映射表
            Map<String, String> tokenMap = buildTokenMap(tokensNode);
            if (tokenMap.isEmpty()) {
                return null;
            }

            // 按token优先级顺序处理
            List<String> orderedTokenIds = new ArrayList<>(tokenMap.keySet());

            // 解析TE2节点数据
            Map<String, TurnIpData> turnIpDataMap = parseTe2Nodes(te2Node);

            // 按token顺序查找匹配的数据
            return findMatchingTurnIpData(orderedTokenIds, tokenMap, turnIpDataMap);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 构建有序token映射表
     */
    private LinkedHashMap<String, String> buildTokenMap(LinkedList<ProtocolTreeNode> tokensNode) {
        return tokensNode.stream()
                .filter(tk -> StringUtils.hasLength(tk.GetAttributeValue("id")) && tk.GetData() != null)
                .collect(Collectors.toMap(
                        tk -> tk.GetAttributeValue("id"),
                        tk -> cn.hutool.core.codec.Base64.encode(tk.GetData()),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    /**
     * 解析TE2节点数据
     */
    private Map<String, TurnIpData> parseTe2Nodes(LinkedList<ProtocolTreeNode> te2Node) {
        Map<String, TurnIpData> turnIpDataMap = new HashMap<>();
        Set<String> processedKeys = new HashSet<>();

        for (ProtocolTreeNode node : te2Node) {
            byte[] ipData = node.GetData();
            if (ipData == null || !isValidIpDataLength(ipData.length)) {
                continue;
            }

            String tokenId = getTokenId(node);
            if (!StringUtils.hasLength(tokenId)) {
                continue;
            }

            // 防重复处理
            String uniqueKey = tokenId + ":" + HexUtil.encodeHexStr(ipData);
            if (processedKeys.contains(uniqueKey)) {
                continue;
            }
            processedKeys.add(uniqueKey);

            // 获取或创建TurnIpData
            TurnIpData turnIpData = turnIpDataMap.computeIfAbsent(tokenId, k -> new TurnIpData());

            // 设置IP地址
            setIpAddress(turnIpData, ipData);
        }

        return turnIpDataMap;
    }

    /**
     * 获取token ID，优先使用token_id，如果为空则使用xtoken_id
     */
    private String getTokenId(ProtocolTreeNode node) {
        String tokenId = node.GetAttributeValue("token_id");
        if (!StringUtils.hasLength(tokenId)) {
            tokenId = node.GetAttributeValue("xtoken_id");
        }
        return tokenId;
    }

    /**
     * 验证IP数据长度是否有效
     */
    private boolean isValidIpDataLength(int length) {
        return length == IPV4_DATA_LENGTH || length == IPV6_DATA_LENGTH;
    }

    /**
     * 根据数据长度设置对应的IP地址
     */
    private void setIpAddress(TurnIpData turnIpData, byte[] ipData) {
        if (ipData.length == IPV4_DATA_LENGTH) {
            turnIpData.setIpv4Addr(ipData);
        } else if (ipData.length == IPV6_DATA_LENGTH) {
            turnIpData.setIpv6Addr(ipData);
        }
    }

    /**
     * 查找匹配的TurnIpData
     */
    private TurnIpData findMatchingTurnIpData(List<String> orderedTokenIds, Map<String, String> tokenMap, Map<String, TurnIpData> turnIpDataMap) {
        for (String tokenId : orderedTokenIds) {
            TurnIpData turnIpData = turnIpDataMap.get(tokenId);
            if (turnIpData != null) {
                String token = tokenMap.get(tokenId);
                turnIpData.setTurnToken(token);
                return turnIpData;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        String hex = "02789cb55b0d901cc57596847e409181e098921c64c20106c4e9b2bbb7bb77e718cc4f241949d8c270406453c3ec4cefeee8666746f3b3b72b59d8b850f15304bb200e2632c2c40463e20a5430e1c79532f9014362c0a108895c49001714c29c0cc6180eb140de7bdd3dd333b37bbaaa54a802a1e9d7ddaf5fbf9fefbd7e3b7b8cb9f4e0874bd8eec357750fbba8b7bc3452aa8c948a23e3a58d2f1dfeded127f967afbfe8b4e3c6f77de7c1ed6fbff2f891bde501734ce66bb665beb370c187d7acf22e5c599e3ce5eb2b7b470651d8b49cf14a61bc38313671e0b5b51fef2d2f8e552ae5f1ea68b938bb7076c5cc711be2bd9abd3f18ab8c558ba3139562a978eef8c4d9a5b1e2ba4279627d61fd39e3ebce39a75ceaad3059db329866d87a10f416970ac5eacca6c9de470dddb68107c38d9cd0efc29f26eb2dba70c3ecefcc2eb97d66756f49b15a281456cf7c82fe3a7335fedfe21fce9c30d95bb670c93b47bc7470d9ecd2d517ac5b7c7ecf1f5d7ec292e6beab9ffcf675fa7de7ee3aea819ffc68db33d7bef995b5cbfe6acd6927ccdcf2d4bf6dde7ccad10b572e183aebf11f3cbda05cde3f79e4f2872fd8ffd51baebca672c96f96dcf8d35f37bff0cd1b974cfdf8b9c7ef5ffbef57de79f355afddd379a87dfd1507cf7df2aefdc7fde9618fbef3876b1fbbe1d6c22d7bf5c5faebaf7ee9f6d9c3663e36f3f175f04773e6a4bf9c5d3473576fe927af3dfe0763bf985d387304fcfbeeece299e533bf37f9fe823ffaf2ce219d19439fda3954637ed7d6e02f1a73f49acd863e3514fa111b1a1e6246d305093875cb640ec8aa6905a12032152afaa0b57443d31d53b3dc40d32353f37cd7d05a2cf42d234888fdc8a1adeaae6dbbd3ccd46a5dcd51c63bbeaed1b6626676bb5dc3437a83b3ad1b5391a705a1ee877c53fc3e544492da34239269a6592dcf86cfe3b0b6c96cbdabd5f400b78521dd6a999a6e7aa116367d163435d6f168853c69cd0a7d3d641a0b42aba587aeaff0554c6400e7b65c4da8313141a3012e61b8ad9ae530cd736dcbe8c2c8a81cb1dd2000d9369af8b190fa0a32c235e28f2dbda3c1811b2c942cd130fe1393584e1f9271a4d8858ca246a370801fcf661d2b445e2a30bbeeeb2d988e9751c5d570a164857289b66801475a5b37b52688bea9fb9ec3029c51490d3a2e4e7574bf2b87a20096c661dcded0da45e542eb01f2530f34249a66208810c40e0ad0744d79a3a87a2ed823fe576fd00184ccd160350f94c0322c4f7742cd6786eb9b5aa0b72da791be1fa2058d819b6c17e722f49969e96014f5107d01fccbd5b12f519fa1986dcbb1424d68a398e5820e0460505a8b2e429d0556e16b6d1873b5c86bf87a3c6e351cd707d5013e61d7c0731d945493399a6b9192072818cf8bf5917fae5b1dcdd7d1c9b98e698596eb48859cb23cdccc803d03585cb71309e04268bc35dcacc6eab8b16e180ccc04155b9ecca14b03bfe05a01c8c77242e6045c990a2315d427370abd28d48800143932515506f810bc79b996a219ae874cd34e7073e04f98ef83889ae067505ca09d2ddd23e59c284f54c7e0bfb016524e6bba0d5b830d04a4c00de4ab140f868687323122df67a0317ae01a53e043b6472c62da14eb0a51e01a7a5d339a0c86d1f44c56d723fed1e71ec2028f072e41d84d91dba19807328e3d6a31f51905092bd4d1fedaa8968ea9d070b6f514ef726cda0a9b311b96d7aee225cb71f4ba536800a0d670279e114af3c3213457740f1d1802ee1938b73eac2361e8b78073748cac8df299d64179401b901a6e152660e4005f25a780d3737421a3806d87e5b74bb7c547c340db6685684c78d3dcd3848adb978609ae9c5c2fcee60e47c4631bccc6ee0adb157c70a28426f93c415fe9da74234411a34c5147fd3ad8839097205024ce12951a8b09fc8e5c842cceb402b2630fee8f4b12f480ae4550792519450c103268ab0b1a8f570ca64f970fc446332641d5c303603ca3a089561284cc132102fd74049c211b244b1b2ec55644e73a0e33d063a2b92726911aca898f5bacdfd6ed80dfff280591e109fec768a5ccffaccad86242e8f35d3829f87cad0581d94fe29b1449b3d6e47a0022d04297cb26e15425f3d104c19581e00d3279751db88e69d79fd2d0fc5bd60e60161d63c773f1600585908b3af3050e0bf214d293dfd1d79330802f87e579f299e782f2d5f5103cba0c0c968db027c35c10f8861644b5c0f02d4ff8d464316179a81b70c968345cab497558cb0bbbc972795a32471bce1c72d8d5e0a11bc9c1c2dc904700cb816bb310ba98beb279d8e1b6236e99849b19b60c1dce85813db632be3a0df2f3e98d064c0d5093b95024c340234218de71ffaf9a19f9c8226744b220b7a8fb8ced80fb6f40783339ac50b8db568b65911d88ea70d86d51100a2f8e5f3d3aaadec610a6239409949df2838a9f4402f2c0b56e289c9132d276312df043d072dd9f820b6a976494b6750f0121ba3f500e44c4903cc0a5b8787504215d1b1d79899bcc207a5c5b2517ae576248d34cc199e9a625900b8ffe0683ab911623e76483630ed0e8f6b4de0d44e4242b09c2c8c9d0388690521ad98bef5c1099a1a00b8310a69c80ec070e45ea1ebba16225858ed520945f8cdc1bad038e73aa3f2f44c33152ca91caf1b6462ce14d92fbcead20a13c4558f42b758bd926fdb50f58f4189aa8ad8740d9d2ea415fa6789c70f4b05ace2c615b183f716dc84fc230736dc225810fe1f9a65808994244c208cbfa4c57905b6e66c05539880c0a1c743479707582af4382938a106363c9b08999148403e98f645cebb3122b310e7b10232a0173542151b0031ea68120b41ed91aa61b99d53a10851c84da1086098625762a48e87ee8b636174be58962b5bae50b17cf4152a914cb85b949aae3d5d10a2749df648a6aac3a3636d19fca17664cc1a09f4a60fa4531dfd32d88b5660713ddd0caa7cd71cc24cd1e3c4c2a9f1b9698557a5ae5c0026f1d52cd6d04e4ae07e1a99ec9666db7d1409f433640d6c09582e70e321ef799002e14d52924d0083a15e4f4b785a95362575c5540f10042b979c700191668084401544ccf75edec623c6863d69df58c2dab8126616a9163810420687a569602821d79563c1b0c6b04c948c573ba884091336bb85e172eb76bbbba99a321f4d264a0d34d48f3200774fd3969e04cbe48362175a6bfd574b0f0bc28e4441460d4d26a9994d47113a4c3a102c67cac5740cc0f985d478a86eb72458882fcfa989e8ac56931f2e7d94d1cc0642e77fb3af75f5656086e68d5bb5c756018034e5d805c11a6653aec61c2055bb654a7afac84539b3a700e3e0af2a5fc20241b11231d26b4d8c75b4238c50cc18e93b9c1c3b103184c025ade874bd03e14389a4966043f83a20394c463ea41040130c11482087304542ea110fc7610431b358d4c3c2db543d3e7b720773dc80bc8c443afd77850cf6906c4a1800295a938fdd2984a4036837e2209ff13259540d14c2403a0894e31557294a4dc1590f4b25597ba4dd03755b9c84e475b861c972b276567e8aec06787d9d590921756c270d0f5732ac8c064060ce13c2b5f9ecc0875066bc34417f6ce61a3cc2c0822040a216d76c0e6dc7a1d97c9e917327748572eca0a716182c3892ca3b1d8626742aa318888eb8dac3a65a8b08e44f9ad880971324df02543ecd6c369427f98ce8a4481c28ac0c1e93b0c3c7007084474678adf71293f28f4de005cdb00e74eae39c86c9b26f53382a5086eb25694fd4e3836b93d1eb1e04205308a01589e712ca0a15cc99bc7a5161959b2db50d98e677efd800464dd4c6f01873cdfec131d05054ab51303f09c020558a40c09f5230ee9e707d5a496aa93224ba21a97abe53c5bd06d21e885c0dcc721ca419385b19c9461507bf4ddb01d243a584445ee7347135498af11841e40155f520abe150be5b13c541c443c01ae6a0e62a0ebe2ede3bd5260f5fa031b60d5ef0800dcef3ae928a01f834c18c661d37addc2d71863400a1339354817a4a967ab283111eb78bcfa8305ecc425ab2404750e01d123cf915516f185f01d649be272b3e026372c5c837d2832518401a6d2fb4d23309806fc86a052a4db192b27ef2e11c640e972aa01838469b84865b826bea9f61d93e2c57a2e4fc633f5d0224fe33b861d998c431fe037f244625ef7dd961697e4c48275ddb241003c8147b3136894dc0dd0c0910226e9a87e440918412660d2b7a8d285fbd62d9bf03e9656a9d2ccab43b489ebd7c0e40984b90431918f5037e4b11af8d61284701fb20a443534542dd78ed25e2ece487009dfb545114e8c8a970a9e2139b284961459a81a8daf22f87252f7a442c2ced2878bcc57a604812cdcb6d14bc84a254cd956d398eedb5d2daea4d1d342d084b8223126d088f7bf12ff1b0665874d27734c9c831305230a0d3d39815db4926a15524c31e6f1ca7bcdb7cc064bcacae9e23199162f54b83e96012430a597321dc2ae287a916ca43a63051deb24c28f07f95ae4a8a052e2bccff85375e4a4de0c881263ad7890846b1791164714f4282b239472601800f50b027a6693f79aa726f7ad1430d29e4aa1476691d8b3511f0b2385d1fee31c9b95e972b3c32a862e021a0715210b1a2d164b87202f29e495ea20627c6c55161ec803d2292b562b79c20015374edfca7d08c879eb36035f27b3fee2dc6432fbcf9001bc9018ce4f0247e29686e25aa4322939082f8d826eed20e5ce4892429e7820524e3c5e2a958ba509419b94e86545423e70c862560c0c809c6d179561017389c3b59542326a6bf14b2facca092a0585a0750882b84086cfcc9e6da4543219afd7535cc5591c041a25a849ef9b224a03caecfab66ecae74d8ed5598e852c8928a7e768c8cbb20e5c2bf89800d52079ae2b2527e60f93bc9caf4a82a663dec01f147292f27c0608016b54941ff1f8816f45f8be04a2f08d21f17ac8e909ecd3a29c9e0e969650847e065b3ce05858018beb39859189f14a9e6c7ba43b22ff438a84005c70a9006a878b0027966ba60494791de29a862fded877e0a2ee5af1dbf920729fb55c7cf09b1e4cd8ffa90f5f72b8e5980367c659b53291caba8e6eda879a05308851cd4a819f6aa97be0c479d3531b498658331cf9bcd26702f6824cb3d85d8d8ec66e650065ecb1aa85b9485b6e0062266bac0c2482c821890a23034e0464a8eb48268c8a0b7ac0de707813e0184f6e851f149d2e0326f042017112f0502b4a14ea6e238326137085201727904a35acff89781e170004208d9372855c0ff437ab796048b61576a52550799ecc261b04fa508b3e94521f023c2e6548aca54324f29a7a7c05a2d041d5e3d4b3c228f57e24c6462ffb89392623b2c7244ba13e1eab90be33e7b8d213c00f8b64220bc7c736f0a7d48c13d888df121fea598d46b7c63d2d07eed417a3d5ed289006e4f182957c9fe137434b8c550a72dc64f5e4558fe22bc90aa521dc00154aa872c5f30aea165060baea2d841ea61c8802bc52213e47d8bf1bc1432df4dbf45a282a2558a7e2405bd618659b1e13c032f178510dc1e8769c0237476fc42abd52da73e5b37889464ce084674358244a4d121684909fe4a2b721cdc1430a96454d9e9fc475a88321056f530439a128c53dd1c8446c886ea6745651e15aa34c91e3894a2983699791b01239a252467d3de9fa5b1f0aee1812dd1dab281ba528139ab26406a726d918ed852aae0a38c00a8026f3ad4c1763eabeb952c3d5299582328d843a5c79530f34a5d3041b203b60faf279489e30a990f28c163948c997fc20e92a428dc422f8e5aa512ba940f2476b5fbdfbf13909fa248c85d48454e153b5e712973f411d9edef1b499788c055395241c9c8964295788211205978b56d5ba46a0096bf816eaada46ea50b8e9cdbf809598a06fd49e062474480cfbe1cba574ae5d238c9c48374951c6ae28a02b44d93ee8026634851eb2129d7e1b6e166b82f264188b297aceb665ba80474f045df49aa5d4d5600654342b6154f8ec7f91f66b199b9d4aa427a6684a2610ebd480255427cf991fd70c821b78386ee89f7345e8990e59462bf29b1b6201e11a51ca1aec975601b96ee5b81288dcab5e27150a5d035b01ca2f6f7e63b2308a1626227eb1c541b421910a890c79aef3ca520d2778e5258911e42358de2fca6ce8b3d48460528c839e230729836ad83aa986e23b1236e6c08f607e917f5850eea94f215220428bcc090f942be0a4bfc5d4719c4fa0ef9717c7e8e0b8138d2a70b428c4ef3f5705d2c2a6961d78b7b8ef8e352fad5290081c79d450ac17c02c020f284b0221aba7d6a86577a3075bbe1c285055332a5cff4676eabc5d9e25841f44b0da57b68781f4e0c8cea908d44be740990f7c98c55d6f2d446f34a5c9dcbd7d86063727a1ae81fa18d52712029a99c598f492b72d9fe0b5594c1ecd4b1d238ca011b7f4d3dd421bb446fadf02c22aaa8bdc1d979836a8a2b1dcb9894129738292f5e8abff0da15440faae1594e52d9a4a6f7a08e2a93a4c3c589b85d9e74ce4308039053578277ae87bf548eaf9c6bf317b1891f511a1a29be1cf05cb6429d9b6b8686d5d198c1610e5995212aed84bc22c45b868bc3244f2e0bd450fc5947e237b8872c212f3bf37b14e5ee734ecf4d46b3e20b0c17150ec36cee5149331f9f9aff08227b6c720283ce477499d1cc6ed5439da39a9742a7cf8e6b864b49092b5f011b9ddf2aa5e1ca5cab94e7b74a85843457352eb3ca7c356bcdff935ee195806dd333b9745d856111ba882e359ab9c2d1c41d51f72bcfdd793b29ddb8ac73cd63d762eafcfff75db13ab1eb3204d9f4638aae2360b351f2793b42ec3048ef130219612561067ae5094d86afb4a947c03c51ead70a8386b5cdc5d1d142b95a4e75c6e5a901b2f177b07c2faaa97b3c6133d20961aa9d92e2363d6cf0fa6512fa32bd1ef229a4cf4ab8014fff62e83e9a1f55e6c94703fee293edd245b3895a340fd3634a9f75ad16f97163323db1591ddefad7a73f25c6b9124496531fd51f368c0b68664820103ff558043bcaf9c144f4d29a44648b933282c422ff4501e302fcdac484f1147d5f926ab22684c59654a7c8f4d25cc8c9093702a82265fe48f4a646a91d84b8403c18c6bf9ae2ed17bcaa2f8197d27b24e9b3ad113c90fbf13b3c2d1d29eb62fe1cf8a1979bca5f192cfe0a0abc2a4a921b223c6819a0a4a212917af94f12757cfce77bf1b2f92e154907b004766c2acc6183a9f83aaf9eda41f473fd7e27cd0371b77308d34604f2fc77067d7ad571276c5d8a7b480115760d9bc51d1a69a279b3d286ed38a2958dc98c1ace448f8bac18e55c0adc73a325df58f3cd28b86cee574d83a4081294bf5448ec910a03c8a0d2da1a242d203b87e25f5481728a85c4a8063ea3c693956271bc5a1ddab56bf6c89968ff646f859a654cf68eed5f02989cd93c39b361edcceeded19b2fb8d4397b53c90cb66e29156b9575f8abe42507ae9a5c11fff47976f1a387afedbd7ac4510bdf5afcc5ead07f2e647f7dfb137b1f6dfe66c53d375dfeceddc7ecfddbebc6bff9f48646f5bdad67aedcf0c0519f0e174d9d5a3edef20fbcf7f54daf2eae9cbfe9f1cab167dcffd97d97edbdf3b6abde7cf1fd8b5e3b7edf45efefdeb5e7995746967ef48efb8a77bc74f7c8b757fdeacedb5e3cea4ba7ed3beb9ff7be75c5f4d98f7fece6e1af9eb8eee7a73cff0ff61b8f6cfdfe49ef3ef7dab133cfbef0e40bbbf76d7cf7a13f39f3926f7cf7efae79fbc3f53f7eec66e7f4ef5cfbb0bdebdc93affcdce5bbdfb60ebfe5c4572fddeb7ce57bde09afdc78d9ba9f7d70cc634ffcf4e4159f9d6eadf997913ddfdafa3fbf7bffed6327efbdf597a74fbef6f2b7aefcaff5dff8edb3cf6ff8d5d4d4d0c39f58712e9e77b2f7233cefe5175f7ae0fc475e78a6f81ffb4ef22fb9e694555b9ff81e73f72ed978e4babbb7ef58fcee2fae3affccfbada7f73cf4cb4746d6dc73de8bd717cf5ac9da7b6e3bb8f6be70fc8707575fbce7fad50fdeb47fe2dea73eb97cdfcca64ee7e5f7ef7c60d5b3ebaf7ee0cfeefacc962b4e1fbb69d735070ffcc5393f7bde7cf8c19b76be77c3110befb8f86b0ffce4a83df6d9bf7fff879b9fbefad7fb3e58f2e2aa373ef8c7999fbfbee3316b367cf996e882ce96efef5879fdbde3c1f675afbcfbd6eee77efb994f9f58eaacfeef8f7ceeefbf3cfdfafebf995db4b7b7d23c7dd2360b9746e56ddd463ddcd2dede094f1fbde08c3366973fd15b86d905184a6f51b5da5b2622fe81c5bbd7f68ee884ee14c3e6dbb5bda5df5d7fe3991ff9f3f94f3866cd61a77ef5cdeb16fcebc1a7ee5db160c1c2699a3c73d8643ca5b768a2b47bf2c6c9ded25bdf58363a7898afb462c13f292b1dfec4e065fa8df55b63516f197a4d7044bd53cffb7c6b5ba1b5f5c26043fbc2c2c673b64fb59bd17abf56e998db2e3caff1f9f18d9b376dfae32d1bddff05846d8e00";
        ProtocolTreeNode node = XmppDecode.decode(HexUtil.decodeHex(hex));
        ProtocolTreeNode offerNode = node.getOneChildren("offer");
        ProtocolTreeNode relayNode = offerNode.getOneChildren("relay");
        LinkedList<ProtocolTreeNode> te2 = relayNode.GetChildren("te2");
        LinkedList<ProtocolTreeNode> token = relayNode.GetChildren("token");
        TurnIpData turnIpData = TE2IpParser.parseTurnIpData(token, te2);
        System.out.println(Base64.encode(turnIpData.getIpv4Addr()));
        System.out.println(Base64.encode(turnIpData.getIpv6Addr()));
        System.out.println(turnIpData);
    }
}