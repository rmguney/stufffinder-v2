<script>
  import * as Card from "$lib/components/ui/card";
  import { onMount } from "svelte";
  import { writable } from "svelte/store";
  import { activeUser } from "../../userStore";
  import { Button } from "$lib/components/ui/button";
  import { Separator } from "$lib/components/ui/separator";

  export let id = '';
  export let title = '';
  export let description = '';
  export let tags = [];
  export let imageSrc = '';
  export let solved = false;
  export let variant = "thumb";
  export let mysteryObject = null;  // Changed from object with defaults to null
  export let postedBy = '';
  export let postedDate = '';
  export let upvotes = 0;
  export let downvotes = 0;
  export let userUpvoted = false;
  export let userDownvoted = false;
  export let createdAt = '';
  export let updatedAt = '';

  let tagDetails = writable([]);
  let currentUser = null; 

  const fetchTagDetails = async () => {
    if (!tags.length) {
      return;
    }
    try {
      let fetchedTags = await Promise.all(tags.map(async (qcode) => {
        const response = await fetch(`https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${qcode}&format=json&languages=en&props=labels|descriptions&origin=*`);
        const data = await response.json();
        const entity = data.entities[qcode];
        return {
          label: entity.labels?.en?.value || 'Unknown label',
          description: entity.descriptions?.en?.value || 'No description',
          id: qcode
        };
      }));
      tagDetails.set(fetchedTags);
    } catch (error) {
      console.error("Failed to fetch tag details:", error);
    }
  };
  
  const toggleResolved = async () => {
    if (currentUser !== postedBy) return;
    try {
        const response = await fetch(`http://localhost:8080/api/posts/${id}/markSolved`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
            }
        });

        if (!response.ok) throw new Error('Failed to toggle resolved status');
        const data = await response.json();
        solved = data.solved;
    } catch (error) {
        console.error("Error toggling resolved status:", error);
    }
};

const formatDate = (isoDate) => {
  if (!isoDate) return "";
  const date = new Date(isoDate);

  const timeOptions = { hour: "numeric", minute: "numeric", hour12: false };
  const timeString = new Intl.DateTimeFormat("en-US", timeOptions).format(date);

  const day = date.getDate();
  const shortMonth = date.toLocaleString("en-US", { month: "short" });
  const fullYear = date.getFullYear();

  return `${timeString}, ${day} ${shortMonth} ${fullYear}`;
};

function handleImageError(event) {
  event.target.src = '/placeholder-image.png';
}

  $: activeUser.subscribe((value) => {
    currentUser = value;
  });

  onMount(() => {
    fetchTagDetails(); 
  });

  const handleVote = async (isUpvote) => {
    if (!currentUser) return; // Must be logged in to vote
    
    try {
      const response = await fetch(`http://localhost:8080/api/posts/${isUpvote ? 'upvote' : 'downvote'}/${id}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (!response.ok) throw new Error('Failed to vote');
      const data = await response.json();
      
      // Update local state
      upvotes = data.upvotes;
      downvotes = data.downvotes;
      userUpvoted = isUpvote && !userUpvoted;
      userDownvoted = !isUpvote && !userDownvoted;
      
    } catch (error) {
      console.error('Error voting:', error);
    }
  };

  // Add a debug log when postedBy changes
  $: {
    if (variant === 'thread') {
      console.log('Posted by:', postedBy);
    }
  }
</script>

<Card.Root class={`shadow-lg hover:shadow-xl transition duration-300
  ${variant === "thumb" ? 'grayscale hover:grayscale-0 bg-opacity-75 hover:bg-opacity-100 w-70 h-70 lg:hover:scale-110' : 'bg-opacity-90 hover:bg-opacity-100'}`}>
  <Card.Header>
    <div class="flex flex-row items-center">
      <div class="flex-1 min-w-0">
        <Card.Title>
          <div class={`${variant === "thumb" ? 'text-ellipsis overflow-hidden whitespace-nowrap w-full max-w-full' : ''}`}>
            {title}
          </div>
        </Card.Title>
        <Card.Description class={`${variant === "thumb" ? 'lg:translate-y-2 text-ellipsis overflow-hidden whitespace-nowrap' : 'my-2.5'}`}>
          <span>
            {#if postedBy}
              <a href={`/user/${postedBy}`} class="hover:text-rose-900 hover:underline font-bold">
                {postedBy}
              </a>
            {:else}
              <span class="font-bold">Anonymous</span>
            {/if}
            at {formatDate(postedDate)}
          </span>
          <div class={`${variant === "thumb" ? 'mt-1' : 'mt-1.5'}`}>
            {#if solved}
              <div class="flex items-center text-teal-800 font-semibold">
                <span class="text-teal-800 font-bold">Resolved</span>
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                </svg>
              </div>
            {:else}
              <div class="flex items-center text-rose-900 font-semibold">
                <span class="text-rose-900 font-bold">Unresolved</span>
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 24 24" stroke-width="2" stroke="currentColor" class="size-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </div>
            {/if}
          </div>
        </Card.Description>
      </div>
    </div>
  </Card.Header>

  <Card.Content>
    {#if variant !== "thumb"}
      <div class="-mt-5">
        <!-- Post metadata section -->
        <div class="flex justify-between items-center mb-4">
          <div class="flex items-center gap-4">
            <!-- Voting section -->
            <div class="flex items-center gap-2">
              <button 
                class="flex items-center gap-1 {userUpvoted ? 'text-green-600' : ''}"
                on:click={() => handleVote(true)}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M5 14l5-5 5 5H5z"/>
                </svg>
                <span>{upvotes}</span>
              </button>
              <button 
                class="flex items-center gap-1 {userDownvoted ? 'text-red-600' : ''}"
                on:click={() => handleVote(false)}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M5 6l5 5 5-5H5z"/>
                </svg>
                <span>{downvotes}</span>
              </button>
            </div>
          </div>
          
          <!-- Creation/Update time -->
          <div class="text-sm text-gray-500">
            {#if updatedAt && updatedAt !== createdAt}
              <span>Updated: {formatDate(updatedAt)}</span>
            {:else}
              <span>Created: {formatDate(createdAt)}</span>
            {/if}
          </div>
        </div>

        <!-- Add description section right after metadata -->
        <div class="mb-4">
          {#if description}
            <p class="text-lg mb-2">{description}</p>
          {/if}
        </div>

        <!-- Tags section -->
        <div>
          <ul>
            <span class="text-md font-semibold text-black dark:text-white">Tags:</span>
            {#each $tagDetails as tag}
              <li>
                <a href={`https://www.wikidata.org/wiki/${tag.id}`} 
                   target="_blank" 
                   rel="noopener noreferrer"
                   class="hover:underline text-black dark:text-white hover:text-rose-700 dark:hover:text-rose-900 text-md">
                  {tag.label}: {tag.description}
                </a>
              </li>
            {/each}
          </ul>
        </div>

        <!-- Mystery Object Details -->
        {#if mysteryObject}
          <Separator class="mt-4 mb-2"/>
          <div class="flex flex-col">
            <ul>
              {#if mysteryObject.description}<li class="mt-2"><span class="font-semibold text-md">Description:</span> {mysteryObject.description}</li>{/if}
              {#if mysteryObject.writtenText}<li class="mt-2"><span class="font-semibold text-md">Written Text:</span> {mysteryObject.writtenText}</li>{/if}
              {#if mysteryObject.color}<li class="mt-2"><span class="font-semibold text-md">Color:</span> {mysteryObject.color}</li>{/if}
              {#if mysteryObject.shape}<li class="mt-2"><span class="font-semibold text-md">Shape:</span> {mysteryObject.shape}</li>{/if}
              {#if mysteryObject.descriptionOfParts}<li class="mt-2"><span class="font-semibold text-md">Parts Description:</span> {mysteryObject.descriptionOfParts}</li>{/if}
              {#if mysteryObject.location}<li class="mt-2"><span class="font-semibold text-md">Location:</span> {mysteryObject.location}</li>{/if}
              {#if mysteryObject.hardness}<li class="mt-2"><span class="font-semibold text-md">Hardness:</span> {mysteryObject.hardness}</li>{/if}
              {#if mysteryObject.timePeriod}<li class="mt-2"><span class="font-semibold text-md">Time Period:</span> {mysteryObject.timePeriod}</li>{/if}
              {#if mysteryObject.smell}<li class="mt-2"><span class="font-semibold text-md">Smell:</span> {mysteryObject.smell}</li>{/if}
              {#if mysteryObject.taste}<li class="mt-2"><span class="font-semibold text-md">Taste:</span> {mysteryObject.taste}</li>{/if}
              {#if mysteryObject.texture}<li class="mt-2"><span class="font-semibold text-md">Texture:</span> {mysteryObject.texture}</li>{/if}
              {#if mysteryObject.value}<li class="mt-2"><span class="font-semibold text-md">Value:</span> ${mysteryObject.value}</li>{/if}
              {#if mysteryObject.originOfAcquisition}<li class="mt-2"><span class="font-semibold text-md">Origin:</span> {mysteryObject.originOfAcquisition}</li>{/if}
              {#if mysteryObject.pattern}<li class="mt-2"><span class="font-semibold text-md">Pattern:</span> {mysteryObject.pattern}</li>{/if}
              {#if mysteryObject.brand}<li class="mt-2"><span class="font-semibold text-md">Brand:</span> {mysteryObject.brand}</li>{/if}
              {#if mysteryObject.print}<li class="mt-2"><span class="font-semibold text-md">Print:</span> {mysteryObject.print}</li>{/if}
              {#if mysteryObject.imageLicenses}<li class="mt-2"><span class="font-semibold text-md">Image Licenses:</span> {mysteryObject.imageLicenses}</li>{/if}
              {#if mysteryObject.handmade}<li class="mt-2"><span class="font-semibold text-md">Handmade:</span> Yes</li>{/if}
              {#if mysteryObject.oneOfAKind}<li class="mt-2"><span class="font-semibold text-md">One of a Kind:</span> Yes</li>{/if}
              {#if mysteryObject.item_condition}<li class="mt-2"><span class="font-semibold text-md">Condition:</span> {mysteryObject.item_condition}</li>{/if}
              {#if mysteryObject.sizeX || mysteryObject.sizeY || mysteryObject.sizeZ}
                <li class="mt-2"><span class="font-semibold text-md">Dimensions:</span> {mysteryObject.sizeX}x{mysteryObject.sizeY}x{mysteryObject.sizeZ} cm</li>
              {/if}
              {#if mysteryObject.weight}<li class="mt-2"><span class="font-semibold text-md">Weight:</span> {mysteryObject.weight}g</li>{/if}
            </ul>
          </div>
        {/if}
      </div>
    {:else}
      <p class="text-sm line-clamp-2">{description || mysteryObject?.description}</p>
    {/if}

    <div class={`${variant === "thumb" ? 'overflow-hidden flex justify-center items-center' : ''}`}>
      <!-- {#if currentUser === postedBy}
        <Button 
          on:click={toggleResolved} 
          class={`${variant === "thumb" ? 'hidden' : 'w-full mt-4 hover:bg-rose-900'}`}>
          {solved ? "Mark as Unresolved" : "Mark as Resolved"}
        </Button>
      {/if} -->
      
      {#if imageSrc}
        {#if variant !== "thumb"}
          <a href={imageSrc} target="_blank" rel="noopener noreferrer">
            <img 
              class="object-cover w-full pt-4" 
              src={imageSrc} 
              alt={title}
              on:error={handleImageError}
            />
          </a>
        {:else}
          <img 
            class="object-cover w-full h-44" 
            src={imageSrc} 
            alt={title}
            on:error={handleImageError}
          />
        {/if}
      {/if}
    </div>
  </Card.Content>
</Card.Root>
