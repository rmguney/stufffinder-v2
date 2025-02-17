<script>
  import { Textarea } from "$lib/components/ui/textarea";
  import { Button } from "$lib/components/ui/button";
  import * as Card from "$lib/components/ui/card";
  import Post from '$lib/components/post.svelte';
  import Comment from '$lib/components/comment.svelte';
  import { threadStore } from '../../../threadStore';
  import { activeUser } from '../../../userStore';

  export let data;
  let comment = '';

  $: commentator = $activeUser || 'Anonymous';

  let handleSend = async () => {
    if (!comment.trim()) {
        console.error("Comment cannot be empty");
        return;
    }

    const endPoint = `https://threef.vercel.app/api/comment/`;
    const payload = {
        thread: data.id,
        comment: comment,
        commentator: commentator,
        postedDateComment: new Date().toISOString(),
    };

    try {
        const response = await fetch(endPoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload),
        });

        if (!response.ok) {
            const errData = await response.json();
            throw new Error(JSON.stringify(errData));
        }

        const newComment = await response.json();

        threadStore.update(prev => {
            return prev.map(thread => {
                if (thread.id == data.id) {
                    const comments = thread.comments ? thread.comments : [];
                    return {
                        ...thread,
                        comments: [...comments, newComment]
                    };
                }
                return thread;
            });
        });

        comment = ''; 
    } catch (error) {
        console.error('Error submitting comment:', error);
    }
  };

  $: thread = $threadStore.find(thread => thread.id == data.id);
</script>

<div class="flex flex-col items-center bg-change dark:bg-dark shifting p-4 lg:p-8">
  <div class="w-full lg:w-2/3">
    <Post 
      id={data.id}
      title={thread.title}
      description={thread.description}
      tags={thread.tags}
      imageSrc={thread.imageSrc}
      postedBy={thread.postedBy}
      postedDate={thread.postedDate}
      material={thread.material}
      size={thread.size}
      shape={thread.shape}
      color={thread.color}
      texture={thread.texture}
      weight={thread.weight}
      smell={thread.smell}
      functionality={thread.functionality}
      period={thread.period}
      location={thread.location}
      variant="thread"
      resolved={thread.resolved}
    />
    
    {#if $activeUser}
    <Card.Root class="bg-opacity-90 hover:bg-opacity-100 p-4 mt-4 flex flex-row justify-center items-center">
      <Textarea bind:value={comment} class="h-20 resize-none p-2" placeholder="Say stuff" />

      <Button on:click={handleSend} class="ml-4 hover:bg-rose-900">Send</Button>
    </Card.Root>
    {:else}
    <Card.Root class="bg-opacity-90 hover:bg-opacity-100 mt-4">
      <Card.Header>
        <Card.Title class="text-lg text-center pb-6">Sign in to comment</Card.Title>
      </Card.Header>
    </Card.Root>
    {/if}
    <div class="flex flex-col justify-center pt-4">
      {#if thread.comments && thread.comments.length > 0}
        <!-- Only display top-level comments (parent is null) -->
        {#each thread.comments.filter(c => !c.parent) as comment}
          <Comment
            commentId={comment.id}
            comment={comment.comment}
            commentator={comment.commentator}
            postedDateComment={comment.postedDateComment}
            selected={comment.selected}
            threadOwner={thread.postedBy}
            replies={comment.replies || []}
          />
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
