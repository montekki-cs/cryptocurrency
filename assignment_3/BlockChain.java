import java.util.ArrayList;
import java.util.HashMap;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */

public class BlockChain {
   public static final int CUT_OFF_AGE = 10;

   // all information required in handling a block in block chain
   private class BlockNode {
      public Block b;
      public BlockNode parent;
      public ArrayList<BlockNode> children;
      public int height;
      // utxo pool for making a new block on top of this block
      private UTXOPool uPool;

      public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
         this.b = b;
         this.parent = parent;
         children = new ArrayList<BlockNode>();
         this.uPool = uPool;
         if (parent != null) {
            height = parent.height + 1;
            parent.children.add(this);
         } else {
            height = 1;
         }
      }

      public UTXOPool getUTXOPoolCopy() {
         return new UTXOPool(uPool);
      }
   }

   private TxHandler txHandler;
   private TransactionPool txPool;
   private BlockNode blockRoot;
   private BlockNode maxHeightBlock;
   private HashMap<byte[], BlockNode> allBlocks;
   private ArrayList<BlockNode> heads;
   private int height;

   /* create an empty block chain with just a genesis block.
    * Assume genesis block is a valid block
    */
   public BlockChain(Block genesisBlock) {
      UTXOPool newPool = new UTXOPool();

      for (int i = 0; i < genesisBlock.getCoinbase().numOutputs(); i++) {
         UTXO utxo = new UTXO(genesisBlock.getCoinbase().getHash(), i);
         newPool.addUTXO(utxo, genesisBlock.getCoinbase().getOutput(i));
      }

      heads = new ArrayList<BlockNode>();
      allBlocks = new HashMap<byte[], BlockNode>();
      txHandler = new TxHandler(newPool);
      blockRoot = new BlockNode(genesisBlock, null, newPool);
      heads.add(blockRoot);

      allBlocks.put(genesisBlock.getHash(), blockRoot);
      maxHeightBlock = blockRoot;
      txPool = new TransactionPool();

      height = 1;
   }

   /* Get the maximum height block
   */
   public Block getMaxHeightBlock() {
      return maxHeightBlock.b;
   }

   /* Get the UTXOPool for mining a new block on top of 
    * max height block
    */
   public UTXOPool getMaxHeightUTXOPool() {
      return maxHeightBlock.getUTXOPoolCopy();
   }

   /* Get the transaction pool to mine a new block
   */
   public TransactionPool getTransactionPool() {
      return txPool;
   }

   /* Add a block to block chain if it is valid.
    * For validity, all transactions should be valid
    * and block should be at height > (maxHeight - CUT_OFF_AGE).
    * For example, you can try creating a new block over genesis block 
    * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1. 
    * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height 2.
    * Return true of block is successfully added
    */
   public boolean addBlock(Block b) {
      if (b.getPrevBlockHash() == null) {
         return false;
      }

      if (allBlocks.containsKey(b.getPrevBlockHash()) == false) {
         return false;
      }

      BlockNode prevNode = allBlocks.get(b.getPrevBlockHash());

      if (prevNode == null) {
         return false;
      }

      if (prevNode.getUTXOPoolCopy() == null) {
         return false;
      }

      TxHandler handler = new TxHandler(prevNode.getUTXOPoolCopy());

      Transaction[] transactions = new Transaction[b.getTransactions().size()];

      b.getTransactions().toArray(transactions);

      Transaction[] res = handler.handleTxs(transactions);

      if (res.length != transactions.length) {
         return false;
      }

      ArrayList<Transaction> txs = b.getTransactions();
      for (Transaction tx : txs) {
         txPool.removeTransaction(tx.getHash());
      }


      UTXOPool newPool = handler.getUTXOPool();
      Transaction coinbase = b.getCoinbase();

      for (int i = 0; i < coinbase.numOutputs(); i++) {
         UTXO utxo = new UTXO(coinbase.getHash(), i);
         newPool.addUTXO(utxo, coinbase.getOutput(0));
      }

      BlockNode newNode = new BlockNode(b, prevNode, newPool);
      allBlocks.put(b.getHash(), newNode);

      if (newNode.height > height) {
         height = prevNode.height;
         maxHeightBlock = newNode;
      }

      if (height - heads.get(0).height > CUT_OFF_AGE) {
         ArrayList<BlockNode> newHeads = new ArrayList<BlockNode>();
         for (BlockNode bHeads : heads) {
            for (BlockNode bChild : bHeads.children) {
               newHeads.add(bChild);
            }
            allBlocks.remove(bHeads.b.getHash());
         }

         heads = newHeads;
      }


      return true;
   }

   /* Add a transaction in transaction pool
   */
   public void addTransaction(Transaction tx) {
      txPool.addTransaction(tx);
   }
}
