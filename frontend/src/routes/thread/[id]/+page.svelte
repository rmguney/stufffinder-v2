<script>
  import { Textarea } from "$lib/components/ui/textarea";
  import { Button } from "$lib/components/ui/button";
  import * as Card from "$lib/components/ui/card";
  import Post from '$lib/components/post.svelte';
  import Comment from '$lib/components/comment.svelte';
  import { threadStore, updateThread, addCommentToThread, loadCommentsForThread } from '../../../threadStore';
  import { activeUser } from '../../../userStore';
  import { onMount, onDestroy } from 'svelte';
  import { getAuthHeader } from '$lib/utils/auth';
  import { PUBLIC_API_URL } from "$env/static/public";

  export let data;
  let comment = '';
  let thread;
  let isLoading = false;
  let error = null;
  let comments = [];

  $: commentator = $activeUser || 'Anonymous';
  
  // This is a separate reactive statement to ensure 'comments' 
  // is updated whenever thread.comments changes
  $: {
    if (thread?.comments) {
      comments = thread.comments;
      console.log("Updated comments array:", comments);
    } else {
      comments = [];
    }
  }

  // Reference to the event listener
  let refreshCommentListener;

  // Function to organize comments into a parent-child hierarchy
  function organizeComments(comments) {
    if (!comments || !Array.isArray(comments)) return [];
    
    console.log("Raw comments to organize:", JSON.stringify(comments, null, 2));
    
    // Since the API already returns comments with their replies nested,
    // we just need to ensure the structure is preserved
    const rootComments = comments.map(comment => ({
      ...comment,
      replies: comment.replies?.map(reply => ({
        ...reply,
        parentCommentId: comment.id // Ensure parent ID is set
      })) || []
    }));

    console.log("Final organized structure:", JSON.stringify(rootComments, null, 2));
    return rootComments;
  }

  onMount(async () => {
    try {
      console.log("Thread ID from data:", data.id);
      
      // Fetch post details
      const response = await fetch(`${PUBLIC_API_URL}/api/posts/getForPostDetails/${data.id}`);
      if (!response.ok) throw new Error('Failed to fetch post details');
      const postData = await response.json();
      console.log("Post data received:", postData);
      
      // Update thread store with post data
      updateThread({
          id: postData.id,
          ...postData,
          mysteryObject: postData.mysteryObject ? {
              description: postData.mysteryObject.description,
              writtenText: postData.mysteryObject.writtenText,
              color: postData.mysteryObject.color,
              shape: postData.mysteryObject.shape,
              descriptionOfParts: postData.mysteryObject.descriptionOfParts,
              location: postData.mysteryObject.location,
              hardness: postData.mysteryObject.hardness,
              timePeriod: postData.mysteryObject.timePeriod,
              smell: postData.mysteryObject.smell,
              taste: postData.mysteryObject.taste,
              texture: postData.mysteryObject.texture,
              value: postData.mysteryObject.value,
              originOfAcquisition: postData.mysteryObject.originOfAcquisition,
              pattern: postData.mysteryObject.pattern,
              brand: postData.mysteryObject.brand,
              print: postData.mysteryObject.print,
              functionality: postData.mysteryObject.functionality,
              imageLicenses: postData.mysteryObject.imageLicenses,
              markings: postData.mysteryObject.markings,
              handmade: postData.mysteryObject.handmade,
              oneOfAKind: postData.mysteryObject.oneOfAKind,
              sizeX: postData.mysteryObject.sizeX,
              sizeY: postData.mysteryObject.sizeY,
              sizeZ: postData.mysteryObject.sizeZ,
              weight: postData.mysteryObject.weight,
              item_condition: postData.mysteryObject.item_condition
          } : null,
          title: postData.title,
          description: postData.description,
          tags: postData.tags || [],
          author: postData.author,
          createdAt: postData.createdAt,
          updatedAt: postData.updatedAt,
          upvotes: postData.upvotes,
          downvotes: postData.downvotes,
          userUpvoted: postData.userUpvoted,
          userDownvoted: postData.userDownvoted,
          solved: postData.solved,
          comments: []
      });

      console.log('Thread ownership debug:', {
        author: postData.author,
        currentUser: $activeUser
      });
      
      // Load and organize comments
      await refreshComments();
      
      // Add event listener for comment refresh requests
      refreshCommentListener = async (event) => {
        if (event.detail?.threadId === data.id) {
          console.log("Comment refresh event received");
          await refreshComments();
        }
      };
      
      document.addEventListener('refreshComments', refreshCommentListener);
      
    } catch (error) {
      console.error('Error fetching thread data:', error);
    }
  });
  
  onDestroy(() => {
    // Clean up the event listener when component is destroyed
    if (refreshCommentListener) {
      document.removeEventListener('refreshComments', refreshCommentListener);
    }
  });

  // Function to refresh comments
  async function refreshComments() {
    try {
      const response = await fetch(`${PUBLIC_API_URL}/api/comments/get/${data.id}`, {
        headers: {
          ...getAuthHeader()
        }
      });
      
      if (!response.ok) throw new Error('Failed to fetch comments');
      const rawComments = await response.json();
      console.log("Raw comments received:", rawComments);
      
      // Process comments to create a hierarchical structure
      const organizedComments = organizeComments(rawComments);
      console.log("Organized comments structure:", organizedComments);
      
      // Update the thread store with organized comments
      threadStore.update(threads => {
        return threads.map(t => {
          if (t.id == data.id) {
            return { ...t, comments: organizedComments };
          }
          return t;
        });
      });
    } catch (error) {
      console.error("Error refreshing comments:", error);
    }
  }

  // Debug function to help visualize the comment structure
  function debugCommentStructure(comments, depth = 0) {
    if (!comments) return "";
    let result = "";
    
    comments.forEach(comment => {
      result += `${'  '.repeat(depth)}ID: ${comment.id}, Content: ${comment.content}\n`;
      if (comment.replies && comment.replies.length > 0) {
        result += `${'  '.repeat(depth)}Replies:\n`;
        result += debugCommentStructure(comment.replies, depth + 1);
      }
    });
    
    return result;
  }

  let handleSend = async () => {
    if (!comment.trim()) {
      console.error("Comment cannot be empty");
      return;
    }

    isLoading = true;
    error = null;
    
    try {
      console.log("Sending comment:", comment, "to post ID:", data.id);
      
      // Use direct API call
      const payload = {
        content: comment,
        postId: data.id,
        parentCommentId: null
      };
      
      const response = await fetch(`${PUBLIC_API_URL}/api/comments/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': localStorage.getItem('authToken') ? `Bearer ${localStorage.getItem('authToken')}` : ''
        },
        body: JSON.stringify(payload)
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to add comment: ${errorText}`);
      }
      
      const newComment = await response.json();
      console.log("Comment submission result:", newComment);
      
      // Refresh comments to ensure we have updated data
      await refreshComments();
      
      // Clear the input
      comment = '';
    } catch (err) {
      console.error('Error submitting comment:', err);
      error = err.message || "Failed to submit comment";
    } finally {
      isLoading = false;
    }
  };

  // Reactive statement to get the current thread from the store
  $: thread = $threadStore.find(thread => thread.id == data.id);
  
  // Update reactive statement to log the comment structure
  $: {
    if (thread) {
      console.log("Current thread:", thread);
      console.log("Thread comments:", thread.comments);
      
      if (comments.length > 0) {
        console.log("Comment hierarchy:");
        console.log(debugCommentStructure(comments));
      }
    }
  }
</script>

<div class="flex flex-col items-center bg-change dark:bg-dark shifting p-4 lg:p-8">
  <div class="w-full lg:w-2/3">
    {#if thread}
      <Post 
        id={data.id}
        title={thread.title}
        description={thread.description}
        tags={thread.tags}
        mysteryObject={thread.mysteryObject}
        imageSrc={thread.mysteryObjectImage ? `data:image/png;base64,${thread.mysteryObjectImage}` : ''}
        postedBy={thread.author}
        postedDate={thread.createdAt}
        upvotes={thread.upvotes}
        downvotes={thread.downvotes}
        userUpvoted={thread.userUpvoted}
        userDownvoted={thread.userDownvoted}
        createdAt={thread.createdAt}
        updatedAt={thread.updatedAt}
        solved={thread.solved}
        variant="thread"
      />
    {/if}
    
    {#if $activeUser}
    <Card.Root class="bg-opacity-90 hover:bg-opacity-100 p-4 mt-4 flex flex-col justify-center items-center">
      <Textarea bind:value={comment} class="w-full h-20 resize-none p-2" placeholder="Say stuff" />
      
      {#if error}
        <p class="text-red-500 mt-2">{error}</p>
      {/if}
      
      <Button on:click={handleSend} class="mt-4 hover:bg-rose-900" disabled={isLoading}>
        {isLoading ? 'Sending...' : 'Send'}
      </Button>
    </Card.Root>
    {:else}
    <Card.Root class="bg-opacity-90 hover:bg-opacity-100 mt-4">
      <Card.Header>
        <Card.Title class="text-lg text-center pb-6">Sign in to comment</Card.Title>
      </Card.Header>
    </Card.Root>
    {/if}
    <div class="flex flex-col justify-center pt-4">
      <pre class="text-xs">Comments: {comments.length}</pre>
      {#if comments.length > 0}
        {#each comments as commentItem (commentItem.id)}
          <div class="mb-2">
            <Comment
              threadId={data.id}
              commentId={commentItem.id}
              comment={commentItem.content} 
              commentator={commentItem.author}
              postedDateComment={commentItem.createdAt}
              selected={commentItem.bestAnswer || false}
              threadOwner={thread?.author}
              replies={commentItem.replies || []}
              upvotes={commentItem.upvotes}
              downvotes={commentItem.downvotes}
              userUpvoted={commentItem.userUpvoted}
              userDownvoted={commentItem.userDownvoted}
            />
          </div>
        {/each}
      {:else}
        <Card.Root class="bg-opacity-90 hover:bg-opacity-100">
          <Card.Header>
            <Card.Title class="pb-2 text-lg text-center">There are no comments yet</Card.Title>
          </Card.Header>
          <Card.Description class="p-4 text-center">
            Be the first to comment!
          </Card.Description>
        </Card.Root>
      {/if}
    </div>
  </div>
</div>
