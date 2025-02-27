<script>
  import Post from '$lib/components/post.svelte';
  import { threadStore } from '../../threadStore';
  import { onMount } from 'svelte';

  onMount(async function () {
    try {
      const response = await fetch('http://localhost:8080/api/posts/getForPostList?page=0&size=100');
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json(); 
      console.log('Raw API response:', data); // Debug log
      threadStore.set(data.content);
    } catch (error) {
      console.error("Error fetching data:", error);
    }
  });
</script>

{#each $threadStore.slice().sort((a, b) => b.id - a.id) as thread}
  <div class="w-full lg:w-[calc(33.333%-1rem)]">
    <a href={`/thread/${thread.id}`}>
      <Post
        id={thread.id}
        title={thread.title}
        description={thread.description}
        tags={thread.tags || []}
        imageSrc={thread.mysteryObjectImage ? `data:image/png;base64,${thread.mysteryObjectImage}` : ''}
        postedBy={thread.author}
        createdAt={thread.createdAt}
        solved={thread.solved}
        mysteryObject={thread.mysteryObject || null}
        variant="thumb"
      />
    </a>
  </div>
{/each}
