package codeu.chat.server;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.User;
import codeu.chat.server.Controller;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

public final class ControllerTest {
	private Controller controller;
	private Model model;
	private Uuid userId;
	private Uuid conversationId;

	@Before
	public void doBefore() {
	    model = new Model();
	    controller = new Controller(Uuid.NULL, model);

	    userId = new Uuid(1);
	    conversationId = new Uuid(2);
	  }

	@Test
	public void testAddUserToConversation() {

		final User user = controller.newUser(userId, "user", Time.now());

	    assertFalse(
	        "Check that user has a valid reference",
	        user == null);
	    assertTrue(
	        "Check that the user has the correct id",
	        Uuid.equals(user.id, userId));

	    final ConversationHeader conversation = controller.newConversation(
        conversationId,
        "conversation",
        user.id,
        Time.now());

	    assertFalse(
	        "Check that conversation has a valid reference",
	        conversation == null);
	    assertTrue(
	        "Check that the conversation has the correct id",
	        Uuid.equals(conversation.id, conversationId));

	    assertTrue(
	    	"Check that the user is already in the conversation",
	    	conversation.userCounter() == 1);

	    assertTrue(
	    	"Check that the current user adding the new user has a valid permission level",
	    	conversation.userCategory.get(userId).equals(3) || 
	    	conversation.userCategory.get(userId).equals(2));
	}

	//Test case that the user cannot change its own permission level
	
	// public void currentPermissionLevel(){
	// 	final Uuid user1Uuid = new Uuid(1);
	// 	final User user = controller.newUser(user1Uuid, "user", Time.now());
	// 	final Uuid user2Uuid = new Uuid(2);
	// 	final User user2 = controller.newUser(user2Uuid, "owner", Time.now());
	// 	final Uuid user3Uuid = new Uuid(3);
	// 	final User user3 = controller.newUser(user3Uuid, "creator", Time.now());

	// 	final ConversationHeader conversation = controller.newConversation(
 //        conversationId,
 //        "conversation",
 //        user3Uuid,
 //        Time.now());

 //        conversation.userCategory.put(user1Uuid, 1);
 //        conversation.userCategory.put(user2Uuid, 2);
 //        //conversation.userCategory.put(user3Uuid, 3);

	// 	//Check that the user is in the conversation
	// 	assertTrue(conversation.userCategory.get(user1Uuid) >= 1);
	// 	assertTrue(conversation.userCategory.get(user2Uuid) >= 1);
	// 	assertTrue(conversation.userCategory.get(user3Uuid) >= 1);

 //        //Check that the users do not change their permission level to their current permission level
 //        assertTrue(conversation.userCategory.get(user1Uuid) == 1);
 //        assertTrue(conversation.userCategory.get(user2Uuid) == 2);
 //        assertTrue(conversation.userCategory.get(user3Uuid) == 3);
	// }

	//General permission level test case
	
	// public void testSameNamePermissionLevelChange() {

	// 	final Uuid user2Uuid = new Uuid(2);
	// 	final User user = controller.newUser(user2Uuid, "Bob", Time.now());
	// 	final Uuid user1Uuid = new Uuid(1);
	// 	final User user1 = controller.newUser(user1Uuid, "Bob", Time.now()); 

	// 	final ConversationHeader conversation = controller.newConversation(
 //        conversationId,
 //        "conversation",
 //        user2Uuid,
 //        Time.now());

 //        conversation.userCategory.put(user1Uuid, 1);

	// 	//Check that the user is in the conversation
	// 	assertTrue(conversation.userCategory.get(userId) >= 1);
	// 	assertTrue(conversation.userCategory.get(user1Uuid) >= 1);

	// 	//Check user will not be able to change permissions even though user has same name as creator
	// 	assertTrue(controller.changePermissionLevel("Bob", "Test Conversation", 1, user1Uuid) == -2);
	// }

	//Test case for permission level change from user to owner 
	@Test
	public void testFromUserToOwner(){
		final User user = controller.newUser(userId, "user", Time.now());

		final ConversationHeader conversation = controller.newConversation(
        conversationId,
        "conversation",
        user.id,
        Time.now());

        conversation.userCategory.put(userId, 1);

        assertTrue(
	    	"Check that the user is already in the conversation",
	    	conversation.userCounter() == 1);

        assertTrue("Check that the user is currently a user",
        	conversation.userCategory.get(userId) == 1); 

	}
	//Test case for permission level change from owner to creator
	
	// public void testFromOwnerToCreator(){
	// 	final Uuid user2Uuid = new Uuid(3);
	// 	final User user = controller.newUser(user2Uuid, "Bob", Time.now());
	// 	final Uuid user1Uuid = new Uuid(2);
	// 	final User user1 = controller.newUser(user1Uuid, "owner", Time.now());

	// 	final ConversationHeader conversation = controller.newConversation(
 //        conversationId,
 //        "conversation",
 //        user2Uuid,
 //        Time.now());

 //        conversation.userCategory.put(user1Uuid, 2);

 //        assertTrue(
	//     	"Check that the user is already in the conversation",
	//     	conversation.userCounter() == 1);

 //        assertTrue("Check that the user is currently a owner",
 //        	conversation.userCategory.get(user1Uuid) == 2); 

	// }
	//Test case for permission level change from user to creator
	@Test
	public void testFromUserToCreator(){
		final User user = controller.newUser(userId, "user", Time.now());

		final ConversationHeader conversation = controller.newConversation(
        conversationId,
        "conversation",
        user.id,
        Time.now());

        conversation.userCategory.put(userId, 1);

        assertTrue(
	    	"Check that the user is already in the conversation",
	    	conversation.userCounter() == 1);

        assertTrue("Check that the user is currently a user",
        	conversation.userCategory.get(userId) == 1); 

	}
}